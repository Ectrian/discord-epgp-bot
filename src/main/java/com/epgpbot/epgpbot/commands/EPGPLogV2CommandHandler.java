package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.epgpbot.schema.RaidType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TablePageSource;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class EPGPLogV2CommandHandler extends CommandHandlerAbstract {
  static class LogEntryFormatOptions {
    boolean showEventId = false;
    boolean showTargetCharacterName = false;
    boolean showNote = false;

    public static LogEntryFormatOptions forRequest(CommandContext context, Request request, Transaction tx) {
      LogEntryFormatOptions opts = new LogEntryFormatOptions();

      if (request.hasFlag("show")) {
        Set<String> vals = new HashSet<>(request.flag("show"));

        if (vals.contains("id")) {
          opts.showEventId = true;
        }

        if (vals.contains("character")) {
          opts.showTargetCharacterName = true;
        }

        if (vals.contains("note")) {
          opts.showNote = true;
        }
      }

      return opts;
    }

    public ImmutableList<String> getColumns() {
      ArrayList<String> columns = new ArrayList<>();
      if (showEventId) {
        columns.add("id");
      }
      columns.add("date");
      columns.add("ep");
      columns.add("gp");
      columns.add("player");
      if (showTargetCharacterName) {
        columns.add("character");
      }
      columns.add("reason");
      columns.add("officer");
      if (showNote) {
        columns.add("note");
      }
      return ImmutableList.copyOf(columns);
    }

    public ImmutableSet<String> getRightJustifiedColumns() {
      HashSet<String> columns = new HashSet<>();
      if (showEventId) {
        columns.add("id");
      }
      columns.add("ep");
      columns.add("gp");
      return ImmutableSet.copyOf(columns);
    }
  }

  static class MergedLogEntry {
    public final List<LogEntry> entries;

    private MergedLogEntry() {
      this.entries = new ArrayList<>();
    }

    private MergedLogEntry(LogEntry entry) {
      this();
      add(entry);
    }

    private MergedLogEntry(List<LogEntry> entries) {
      this.entries = entries;
    }

    public int size() {
      return entries.size();
    }

    public void add(LogEntry entry) {
      entries.add(entry);
    }

    public LogEntry head() {
      return entries.get(0);
    }

    private Map<String, Object> formatRow(LogEntryFormatOptions opts) {
      Map<String, Object> row = new HashMap<>();
      LogEntry first = head();
      Map<String, Integer> targetPlayerNames = new LinkedHashMap<>();
      Map<String, Integer> targetCharacterNames = new LinkedHashMap<>();

      for (LogEntry entry : entries) {
        if (!targetPlayerNames.containsKey(entry.targetPlayerName)) {
          targetPlayerNames.put(entry.targetPlayerName, 0);
        }
        targetPlayerNames.put(entry.targetPlayerName, targetPlayerNames.get(entry.targetPlayerName) + 1);

        if (entry.targetCharacterName.isPresent()) {
          String targetCharacterName = entry.targetCharacterName.get();
          if (!targetCharacterNames.containsKey(targetCharacterName)) {
            targetCharacterNames.put(targetCharacterName, 0);
          }
          targetCharacterNames.put(targetCharacterName, targetCharacterNames.get(targetCharacterName) + 1);
        }
      }

      if (opts.showEventId) {
        if (first.eventType == EPGPEventType.DECAY && entries.size() > 1) {
          row.put("id", "*");
        } else {
          Iterator<Long> eventIds = entries.stream().map((e) -> e.eventId).iterator();
          row.put("id", Joiner.on(", ").join(eventIds));
        }
      }

      List<String> playerNames = new ArrayList<>();
      for (Map.Entry<String, Integer> entry : targetPlayerNames.entrySet()) {
        if (entry.getValue() == 1) {
          playerNames.add(entry.getKey());
        } else {
          playerNames.add(String.format("%s (x%d)", entry.getKey(), entry.getValue()));
        }
      }
      row.put("player", Joiner.on(", ").join(playerNames));

      if (opts.showTargetCharacterName) {
        List<String> characterNames = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : targetCharacterNames.entrySet()) {
          if (entry.getValue() == 1) {
            characterNames.add(entry.getKey());
          } else {
            characterNames.add(String.format("%s (x%d)", entry.getKey(), entry.getValue()));
          }
        }
        row.put("character", Joiner.on(", ").join(characterNames));
      }

      row.put("date", first.getFormattedDate());
      row.put("officer", first.sourcePlayerName);

      if (first.epDelta != 0) {
        if (first.epDelta > 0) {
          row.put("ep", String.format("+%d", first.epDelta));
        } else {
          row.put("ep", first.epDelta);
        }
      }

      if (first.gpDelta != 0) {
        if (first.gpDelta > 0) {
          row.put("gp", String.format("+%d", first.gpDelta));
        } else {
          row.put("gp", first.gpDelta);
        }
      }

      if (opts.showNote &&
          first.note.isPresent() &&
          first.eventType != EPGPEventType.PENALTY &&
          first.eventType != EPGPEventType.INCENTIVE) {
        row.put("note", first.note.get());
      }

      if (first.eventType == EPGPEventType.DECAY) {
        if (targetPlayerNames.size() > 1) {
          row.put("ep", "*");
          row.put("gp", "*");
          row.put("player", String.format("(%d players)", targetPlayerNames.size()));
        }
        if (opts.showTargetCharacterName) {
          row.put("character", "*");
        }
        row.put("reason", "Decay");
      }

      else if (first.eventType == EPGPEventType.PENALTY) {
        row.put("reason", String.format("Penalty (%s)", first.note.orElse("UNKNOWN REASON")));
      }

      else if (first.eventType == EPGPEventType.INCENTIVE) {
        row.put("reason", String.format("Incentive (%s)", first.note.orElse("UNKNOWN REASON")));
      }

      else if (first.eventType == EPGPEventType.LOOT) {
        row.put("reason", String.format("Loot (%s)", first.lootName.orElse("UNKNOWN ITEM")));
      }

      else if (first.eventType == EPGPEventType.RAID) {
        row.put("reason", String.format("Raid (%s)", (first.raidType.isPresent() ? first.raidType.get() : "UNKNOWN RAID")));
      }

      else if (first.eventType == EPGPEventType.STANDBY) {
        row.put("reason", String.format("Standby (%s)", (first.raidType.isPresent() ? first.raidType.get() : "UNKNOWN RAID")));
      }

      else if (first.eventType == EPGPEventType.HIDDEN) {
        row.put("reason", String.format("Hidden"));
      }

      else {
        row.put("reason", first.eventType);
      }

      if (first.undoes.isPresent()) {
        row.put("reason", String.format("%s (UNDO)", row.get("reason")));
      }
      if (first.undoneBy.isPresent()) {
        row.put("reason", String.format("%s*", row.get("reason")));
      }

      return row;
    }

    public static List<MergedLogEntry> fromEntries(List<LogEntry> entries, LogEntryFormatOptions opts) {
      List<MergedLogEntry> out = new ArrayList<>();
      MergedLogEntry lastEntry = null;

      for (LogEntry entry : entries) {
        if (lastEntry != null) {
          if (entry.canMergeInto(lastEntry, opts)) {
            lastEntry.add(entry);
            continue;
          }
        }

        lastEntry = new MergedLogEntry(entry);
        out.add(lastEntry);
      }

      return out;
    }

    public static List<Map<String, Object>> formatTable(List<MergedLogEntry> entries, LogEntryFormatOptions opts) {
      List<Map<String, Object>> result = new ArrayList<>();

      for (MergedLogEntry entry : entries) {
        result.add(entry.formatRow(opts));
      }

      return result;
    }
  }

  static class LogEntry {
    public long eventId;
    public long timestamp;
    public long targetPlayerId;
    public String targetPlayerName;
    public Optional<Long> targetCharacterId;
    public Optional<String> targetCharacterName;
    public long sourcePlayerId;
    public String sourcePlayerName;
    public EPGPEventType eventType;
    public Optional<RaidType> raidType;
    public Optional<Long> lootId;
    public Optional<String> lootName;
    public long epDelta;
    public long gpDelta;
    public Optional<String> note;
    public Optional<Long> undoneBy;
    public Optional<Long> undoes;

    public String getFormattedDate() {
      return formatDate(timestamp);
    }

    public static LogEntry readNext(Cursor r) throws Exception {
      LogEntry entry = new LogEntry();
      entry.eventId = r.get("id", Long.class);
      entry.timestamp = r.get("timestamp", Long.class);
      entry.targetPlayerId = r.get("target_player_id", Long.class);
      entry.targetPlayerName = r.get("target_player_name", String.class);
      entry.sourcePlayerId = r.get("source_player_id", Long.class);
      entry.sourcePlayerName = r.get("source_player_name", String.class);
      entry.targetCharacterId = r.getNullable("target_character_id", Long.class);
      entry.targetCharacterName = r.getNullable("target_character_name", String.class);
      entry.eventType = EPGPEventType.values()[r.get("type", Integer.class)];
      entry.epDelta = r.get("ep_delta", Long.class);
      entry.gpDelta = r.get("gp_delta", Long.class);
      entry.note = r.getNullable("note", String.class);
      entry.lootName = r.getNullable("loot_name", String.class);
      entry.lootId = r.getNullable("loot_id", Long.class);
      entry.raidType = r.getNullable("raid_type", Integer.class).map((i) -> RaidType.values()[i]);
      entry.undoes = r.getNullable("undoes", Long.class);
      entry.undoneBy = r.getNullable("undone_by", Long.class);
      return entry;
    }

    public boolean canMergeInto(MergedLogEntry mergedEntry, LogEntryFormatOptions opts) {
      if (mergedEntry.size() == 0) {
        return true;
      }

      LogEntry other = mergedEntry.head();

      if (this.eventType == EPGPEventType.DECAY &&
          this.eventType == other.eventType &&
          this.sourcePlayerId == other.sourcePlayerId &&
          Math.abs(other.timestamp - this.timestamp) < 600) {
        return true;
      }

      if (opts.showEventId || opts.showTargetCharacterName) {
        return false;
      }

      if (mergedEntry.size() >= 10) {
        return false;  // Keep row size small.
      }

      return (!other.undoneBy.isPresent()) &&
             (!this.undoneBy.isPresent()) &&
             (!other.undoes.isPresent()) &&
             (!this.undoes.isPresent()) &&
             (other.epDelta == this.epDelta) &&
             (other.gpDelta == this.gpDelta) &&
             (other.note.equals(this.note)) &&
             (other.raidType.equals(this.raidType)) &&
             (other.lootId.equals(this.lootId)) &&
             (other.eventType == this.eventType) &&
             (other.sourcePlayerId == this.sourcePlayerId) &&
             Math.abs(other.timestamp - this.timestamp) < 600;
    }
  }

  static class LogEntryFilter {
    public Optional<Set<Long>> eventIds = Optional.empty();
    public Optional<Long> timestampFrom = Optional.empty();
    public Optional<Long> timestampUntil = Optional.empty();
    public Optional<Set<Long>> targetPlayerIds = Optional.empty();
    public Optional<Set<Long>> sourcePlayerIds = Optional.empty();
    public Optional<Set<EPGPEventType>> eventTypes = Optional.empty();
    public Optional<Set<RaidType>> raidTypes = Optional.empty();
    public Optional<Set<Long>> lootIds = Optional.empty();
    public boolean includeUndos = false;

    private Statement query(Transaction tx, long offset, long limit) throws Exception {
      List<String> filters = new ArrayList<>();

      // TODO: raidTypes
      // TODO: timestampFrom
      // TODO: timestampUntil

      if (eventIds.isPresent()) {
        filters.add("l.id IN " + Statement.array("event_ids", eventIds.get()));
      }

      if (lootIds.isPresent()) {
        filters.add("l.loot_id IN " + Statement.array("loot_ids", lootIds.get()));
      }

      if (targetPlayerIds.isPresent()) {
        filters.add("l.target_player_id IN " + Statement.array("target_player_ids", targetPlayerIds.get()));
      }

      if (sourcePlayerIds.isPresent()) {
        filters.add("l.source_player_id IN " + Statement.array("source_player_ids", sourcePlayerIds.get()));
      }

      if (eventTypes.isPresent() &&
          !eventTypes.get().isEmpty()) {
        filters.add("l.type IN " + Statement.array("event_types", eventTypes.get()));
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
        + "  lt.name AS loot_name "
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
        + "ORDER BY timestamp DESC, id DESC "
        + "LIMIT %d,%d;",
        whereClause, offset, limit);
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

      if (request.hasFlag("show")) {
        if (request.flag("show").contains("undo")) {
          filter.includeUndos = true;
        }
      }

      if (!context.user().hasPermission(PermissionType.AUDIT_EPGP) ||
          !request.hasFlag("show-hidden")) {
        filter.eventTypes.get().remove(EPGPEventType.HIDDEN);
      }

      return filter;
    }

    private boolean matches(LogEntry entry) {
      if (eventIds.isPresent()) {
        if (!eventIds.get().contains(entry.eventId)) {
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

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    try (Transaction tx = context.database().transaction()) {
      LogEntryFilter filter = LogEntryFilter.forRequest(context, request, tx);
      LogEntryFormatOptions format = LogEntryFormatOptions.forRequest(context, request, tx);
      List<LogEntry> entries = filter.fetch(tx, 0, 5000);
      List<MergedLogEntry> mergedEntries = MergedLogEntry.fromEntries(entries, format);
      context.replyWithPages(
          new TablePageSource(
            "EPGP Log",
            MergedLogEntry.formatTable(mergedEntries, format),
            format.getColumns(),
            format.getRightJustifiedColumns()));
    }
  }

  @Override
  public String help() {
    // Undocumented: --show-hidden
    return "[<<...character:string>|'<all>'>] [--id <...id:int>] [--type <...type:string>] [--officer <...character:string>] [--loot <...loot:string> [--show <...{id|character|note|undo}>] - Displays EPGP logs.";
  }

  @Override
  public String command() {
    return "epgp.log";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
