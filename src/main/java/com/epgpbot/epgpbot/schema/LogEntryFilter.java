package com.epgpbot.epgpbot.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.commands.EPGPLogV2CommandHandler;
import com.epgpbot.epgpbot.schema.game.ItemSlotGroup;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.PlayerId;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

public class LogEntryFilter {
  public Optional<Set<Long>> eventIds = Optional.empty();
  public Optional<Long> timestampFrom = Optional.empty();
  public Optional<Long> timestampUntil = Optional.empty();
  public Optional<Set<Long>> targetPlayerIds = Optional.empty();
  public Optional<Set<Long>> sourcePlayerIds = Optional.empty();
  public Optional<Set<EPGPEventType>> eventTypes = Optional.empty();
  public Optional<Set<RaidType>> raidTypes = Optional.empty();
  public Optional<Set<Long>> lootIds = Optional.empty();
  public Optional<Set<ItemSlotGroup>> slots = Optional.empty();
  public boolean includeUndos = false;
  public String sortBy = "timestamp";

  private Statement query(Transaction tx, long offset, long limit) throws Exception {
    List<String> filters = new ArrayList<>();

    // TODO: raidTypes
    // TODO: timestampFrom
    // TODO: timestampUntil
    // TODO: slots

    if (eventIds.isPresent()) {
      filters.add("l.id IN " + Statement.in("event_ids", eventIds.get()));
    }

    if (lootIds.isPresent()) {
      filters.add("l.loot_id IN " + Statement.in("loot_ids", lootIds.get()));
    }

    if (targetPlayerIds.isPresent()) {
      filters.add("l.target_player_id IN " + Statement.in("target_player_ids", targetPlayerIds.get()));
    }

    if (sourcePlayerIds.isPresent()) {
      filters.add("l.source_player_id IN " + Statement.in("source_player_ids", sourcePlayerIds.get()));
    }

    if (eventTypes.isPresent() &&
        !eventTypes.get().isEmpty()) {
      filters.add("l.type IN " + Statement.in("event_types", eventTypes.get()));
    }

    if (!includeUndos) {
      filters.add("l.undoes IS NULL");
      filters.add("l.undone_by IS NULL");
    }


    String whereClause = "";
    if (!filters.isEmpty()) {
      whereClause = "WHERE " + Joiner.on(" AND ").join(filters);
    }

    String s = String.format(
        "SELECT "
      + "  l.*, "
      + "  sp.name AS source_player_name, "
      + "  tp.name AS target_player_name, "
      + "  tc.name AS target_character_name, "
      + "  lt.name AS loot_name,"
      + "  (SELECT lgi.game_slot FROM loot_game_info AS lgi WHERE lgi.loot_id = lt.id LIMIT 1) AS game_slot "
      + "FROM "
      + "  epgp_log AS l "
      + "JOIN "
      + "  players AS sp ON sp.id = l.source_player_id "
      + "JOIN "
      + "  players AS tp ON tp.id = l.target_player_id "
      + "LEFT JOIN "
      + "  characters AS tc ON tc.id = l.target_character_id "
      + "LEFT JOIN "
      + "  loot AS lt ON lt.id = l.loot_id "
      + "%s "
      + "ORDER BY %s DESC, id DESC "
      + "LIMIT %d,%d;",
      whereClause, sortBy, offset, limit);
    Statement q = tx.prepare(s);

    if (eventIds.isPresent()) {
      q.bindArray("event_ids", eventIds.get());
    }

    if (lootIds.isPresent()) {
      q.bindArray("loot_ids", lootIds.get());
    }

    if (targetPlayerIds.isPresent()) {
      q.bindArray("target_player_ids", targetPlayerIds.get());
    }

    if (sourcePlayerIds.isPresent()) {
      q.bindArray("source_player_ids", sourcePlayerIds.get());
    }

    if (eventTypes.isPresent() &&
        !eventTypes.get().isEmpty()) {
      Set<Integer> eventTypeOrdinals = eventTypes
          .get()
          .stream()
          .map(i -> i.ordinal())
          .collect(Collectors.toSet());
      q.bindArray("event_types", eventTypeOrdinals);
    }

    return q;
  }

  private static Optional<Set<Long>> buildPlayerIdList(CommandContext context, Transaction tx, List<String> characterNames) throws Exception {
    if (characterNames.isEmpty()) {
      return Optional.empty();  // No filtering.
    }

    Set<Long> playerIds = new HashSet<>();

    for (String characterName : characterNames) {
      if ("<all>".equals(characterName)) {
        return Optional.empty();  // No filtering.
      }

      PlayerId player = EPGPLogV2CommandHandler.getPlayerIdForCharacter(tx, characterName);

      if (player == null) {
        context.abort("Unknown character '%s'.", characterName);
      }

      playerIds.add(player.id);
    }

    return Optional.of(playerIds);
  }

  public static LogEntryFilter forRequest(CommandContext context, Request request, Transaction tx) throws Exception {
    LogEntryFilter filter = new LogEntryFilter();

    if (request.arguments().isEmpty()) {
      PlayerId player = EPGPLogV2CommandHandler.getInferredPlayer(tx, context);

      if (player == null) {
        context.abort("I'm not sure who you are - try using '!epgp.log <character>' instead.");
      }

      filter.targetPlayerIds = Optional.of(ImmutableSet.of(player.id));
    } else {
      filter.targetPlayerIds = buildPlayerIdList(context, tx, request.arguments());
    }

    if (request.hasFlag("officer")) {
      filter.sourcePlayerIds = buildPlayerIdList(context, tx, request.flag("officer"));
    }

    if (request.hasFlag("id")) {
      Set<Long> eventIds = new HashSet<>();

      for (String id : request.flag("id")) {
        try {
          eventIds.add(Long.parseLong(id));
        } catch (NumberFormatException e) {
          context.abort("Unable to parse integer '%s'.", id);
        }
      }

      filter.eventIds = Optional.of(eventIds);
    }

    if (request.hasFlag("loot")) {
      Set<Long> lootIds = new HashSet<>();

      for (String lootName : request.flag("loot")) {
        LootInfo loot = LootInfo.searchForSingleMatch(tx, lootName);

        if (loot == null) {
          context.abort("Unknown or ambiguous item '%s' - try using !item.search to find an exact name.", lootName);
        }

        lootIds.add(loot.lootId);
      }

      filter.lootIds = Optional.of(lootIds);
    }

    filter.eventTypes = Optional.of(new HashSet<>(ImmutableSet.copyOf(EPGPEventType.values())));
    filter.eventTypes.get().remove(EPGPEventType.EQUIP);

    if (request.hasFlag("type")) {
      Set<EPGPEventType> eventTypes = new HashSet<>();

      for (String type : request.flag("type")) {
        EPGPEventType eventType = EPGPEventType.forString(type);
        if (eventType == null) {
          context.abort("Unknown type '%s' - expected {%s}.", type, EPGPEventType.docChoices());
        }
        eventTypes.add(eventType);
      }

      filter.eventTypes = Optional.of(eventTypes);
    }

    if (request.hasFlag("slot") ) {
      Set<ItemSlotGroup> slots = new HashSet<>();

      for (String type : request.flag("slot")) {
        ItemSlotGroup slot = ItemSlotGroup.forString(type);
        if (slot == null) {
          context.abort("Unknown type '%s' - expected {%s}.", type, ItemSlotGroup.docChoices());
        }
        slots.add(slot);
      }

      filter.slots = Optional.of(slots);
    }

    if (request.hasFlag("show")) {
      if (request.flag("show").contains("undo")) {
        filter.includeUndos = true;
      }
      if (request.flag("show").contains("equip")) {
        filter.eventTypes.get().add(EPGPEventType.EQUIP);
      }
    }

    if (!context.user().hasPermission(PermissionType.AUDIT_EPGP) ||
        !request.hasFlag("show-hidden")) {
      filter.eventTypes.get().remove(EPGPEventType.HIDDEN);
    }

    if (request.hasFlag("sort")) {
      if (request.flag("sort").contains("time")) {
        filter.sortBy = "timestamp";
      }
      if (request.flag("sort").contains("action_time")) {
        filter.sortBy = "action_timestamp";
      }
    }

    return filter;
  }

  private boolean matches(LogEntry entry) {
    if (eventIds.isPresent()) {
      if (!eventIds.get().contains(entry.eventId)) {
        return false;
      }
    }

    if (slots.isPresent()) {
      boolean match = false;

      if (entry.slot.isPresent()) {
        for (ItemSlotGroup isg : slots.get()) {
          if (isg.slots.contains(entry.slot.get())) {
            match = true;
            break;
          }
        }
      }

      if (!match) {
        return false;
      }
    }

    if (targetPlayerIds.isPresent()) {
      if (!targetPlayerIds.get().contains(entry.targetPlayerId)) {
        return false;
      }
    }

    if (sourcePlayerIds.isPresent()) {
      if (!sourcePlayerIds.get().contains(entry.sourcePlayerId)) {
        return false;
      }
    }

    if (eventTypes.isPresent()) {
      if (!eventTypes.get().contains(entry.eventType)) {
        return false;
      }
    }

    if (raidTypes.isPresent()) {
      if (!entry.raidType.isPresent()) {
        return false;
      }
      if (!raidTypes.get().contains(entry.raidType.get())) {
        return false;
      }
    }

    if (lootIds.isPresent()) {
      if (!entry.lootId.isPresent()) {
        return false;
      }
      if (!lootIds.get().contains(entry.lootId.get())) {
        return false;
      }
    }

    if (timestampFrom.isPresent()) {
      if (entry.timestamp < timestampFrom.get()) {
        return false;
      }
    }

    if (timestampUntil.isPresent()) {
      if (entry.timestamp > timestampUntil.get()) {
        return false;
      }
    }

    if (!includeUndos) {
      if (entry.undoes.isPresent()) {
        return false;
      }
      if (entry.undoneBy.isPresent()) {
        return false;
      }
    }

    return true;
  }

  public List<LogEntry> fetch(Transaction tx, long offset, long limit) throws Exception {
    List<LogEntry> result = new ArrayList<>();
    int i = 0;

    try (Statement q = query(tx, offset, limit)) {
      try (Cursor r = q.executeFetch()) {
        while (r.next()) {
          LogEntry entry = LogEntry.readNext(r);
          if (matches(entry)) {
            if (i >= offset) {
              result.add(entry);
              if (result.size() >= limit) {
                break;
              }
            }
            i++;
          }
        }
      }
    }

    return result;
  }
}