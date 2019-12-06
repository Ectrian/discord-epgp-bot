package com.epgpbot.epgpbot.commands;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.database.mysql.SQLNull;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.epgpbot.schema.RaidType;
import com.epgpbot.epgpbot.schema.game.ItemRarity;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TablePageSource;
import com.epgpbot.util.MessagePaginator.PageSource;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import net.dv8tion.jda.core.EmbedBuilder;

public abstract class AbstractEPGPCommandHandler extends CommandHandlerAbstract {
  protected static final int EPGP_LOG_FETCH_LIMIT = 5000;
  protected static final int EPGP_LOG_DISPLAY_LIMIT = 20;

  protected long getSourcePlayerId(CommandContext context) throws Exception {
    if (!context.user().hasPlayer()) {
      context.reply("You must link yourself to a player using !player.link before using this command.");
      throw new Exception("No user linked.");
    }
    return context.user().playerId();
  }


  protected Long getLootId(Transaction tx, String name) throws Exception {
    return getLootId(tx, name, false);
  }

  protected Long getLootId(Transaction tx, String name, boolean create) throws Exception {
    try {
      long id = Long.parseLong(name);
      try (Statement q = tx.prepare("SELECT loot_id FROM loot_game_info WHERE game_id = :id;")) {
        q.bind("id", id);
        try (Cursor r = q.executeFetch()) {
          if (r.next()) {
            return r.get("loot_id", Long.class);
          }
        }
      }
    } catch (NumberFormatException e) {}

    try (Statement q = tx.prepare("SELECT id FROM loot WHERE name = :name;")) {
      q.bind("name", name);
      try (Cursor r = q.executeFetch()) {
        if (r.next()) {
          return r.get("id", Long.class);
        }
      }
    }

    try (Statement q = tx.prepare("SELECT loot_id FROM loot_alias WHERE name = :name;")) {
      q.bind("name", name);
      try (Cursor r = q.executeFetch()) {
        if (r.next()) {
          return r.get("loot_id", Long.class);
        }
      }
    }

    if (!create) {
      return null;
    }

    try (Statement q = tx.prepare("INSERT INTO loot (name) VALUES (:name);")) {
      q.bind("name", name);
      return q.executeInsert();
    }
  }

  protected String getLootName(Transaction tx, long id) throws Exception
  {
    try (Statement q = tx.prepare("SELECT name FROM loot WHERE id = :id;")) {
      q.bind("id", id);
      try (Cursor r = q.executeFetch()) {
        if (r.next()) {
          return r.get("name", String.class);
        }
      }
    }
    throw new Exception("Loot not found.");
  }

  public static class EPGPLogFilter { // TODO: Implement.
    public Set<Long> sourcePlayerIds = ImmutableSet.of(); // player names, transport mentions, character names
    public Set<Long> targetPlayerIds = ImmutableSet.of(); // player names, transport mentions, character names
    public Set<Long> lootIds = ImmutableSet.of(); // loot names, loot aliases
    public Set<EPGPEventType> eventTypes = ImmutableSet.of();
    public Set<RaidType> raidTypes = ImmutableSet.of();
    public boolean showHidden = false;
    public long offset = 0;
    public long limit = 20;

    public static EPGPLogFilter from(CommandContext context, Request request, Transaction tx) {
      EPGPLogFilter filter = new EPGPLogFilter();

      if (request.hasFlag("source")) {

      }

      if (request.hasFlag("target")) {

      }

      if (request.hasFlag("loot")) {

      }

      if (request.hasFlag("type")) {

      }

      if (request.hasFlag("raid")) {

      }

      if (request.hasFlag("limit")) {

      }

      if (request.hasFlag("page")) {

      }

      filter.showHidden = (request.hasFlag("show-hidden") &&
                           context.user().hasPermission(PermissionType.AUDIT_EPGP));

      return filter;
    }

    protected void addFilters(Map<String, Object> params, Set<String> clauses) {
      // TODO
    }

    public Cursor execute(Transaction tx) throws Exception {
      Map<String, Object> params = new HashMap<>();
      Set<String> clauses = new HashSet<>();

      addFilters(params, clauses);

      String whereClause = "";
      if (!clauses.isEmpty()) {
        whereClause = "WHERE " + Joiner.on(" AND ").join(clauses);
      }
      String query = String.format(LOG_QUERY_FMT, whereClause, EPGP_LOG_FETCH_LIMIT);

      try (Statement q = tx.prepare(query)) {
        for (Map.Entry<String, Object> param : params.entrySet()) {
          q.bind(param.getKey(), param.getValue());
        }
        return q.executeFetch();
      }
    }

    public boolean matches(EPGPLogEntry entry) {
      if (entry.eventType == EPGPEventType.HIDDEN && !showHidden) {
        return false;
      }

      // Set<Long> sourcePlayerIds
      // Set<Long> targetPlayerIds
      // Set<Long> lootIds
      // Set<EPGPEventType> eventTypes
      // Set<RaidType> raidTypes

      return false; // TODO
    }
  }

  protected void performEPGPUpdate(CommandContext context, Transaction tx, EPGPEventType event,
                                   long epDelta, long gpDelta, String lootName, String raidName,
                                   String note, List<String> characters, List<String> charactersToCheckForDuplicates,
                                   boolean createLoot) throws Exception {
    // TODO: use charactersToCheckForDuplicates.
    Long lootId = -1L;
    if (lootName != null) {
      lootId = getLootId(tx, lootName, createLoot);
      if (lootId == null) {
        sendError(context, "Unknown loot '%s' - check for typo or use --add-loot to override.", lootName);
        return;
      }
    }

    RaidType raidType = null;
    if (raidName != null) {
      try {
        raidType = RaidType.valueOf(raidName);
      } catch (IllegalArgumentException e) {
        context.reply(String.format("Unknown raid type '%s' - expected one of {%s}",
                                    raidName,
                                    Joiner.on("|").join(RaidType.values())));
        return;
      }
    }

    List<String> unknownCharacters = new ArrayList<>();
    List<String> unlinkedCharacters = new ArrayList<>();
    Map<Long, Set<String>> duplicatePlayerIds = new HashMap<>();
    Map<Long, String> characterIds = new HashMap<>();
    Map<Long, Long> playerIds = new HashMap<>();

    for (String characterName : characters) {
      long playerId, characterId;

      try (Statement q = tx.prepare("SELECT id, player_id FROM characters WHERE lower(name) = :name;")) {
        q.bind("name", characterName);
        try (Cursor r = q.executeFetch()) {
          if (!r.next()) {
            unknownCharacters.add(characterName);
            continue;
          }
          Optional<Long> playerIdOpt = r.getNullable("player_id", Long.class);
          if (!playerIdOpt.isPresent()) {
            unlinkedCharacters.add(characterName);
            continue;
          }
          characterId = r.get("id", Long.class);
          playerId = playerIdOpt.get();
        }
      }

      characterIds.put(characterId, characterName);

      if (playerIds.containsKey(playerId)) {
        if (!duplicatePlayerIds.containsKey(playerId)) {
          duplicatePlayerIds.put(playerId, new HashSet<>());
        }
        duplicatePlayerIds.get(playerId).add(characterIds.get(playerIds.get(playerId)));
        duplicatePlayerIds.get(playerId).add(characterName);
        continue;
      }

      playerIds.put(playerId, characterId);
    }

    StringBuilder errorMessage = new StringBuilder();

    if (!unknownCharacters.isEmpty()) {
      for (String c : unknownCharacters) {
        errorMessage.append(String.format("Unknown character '%s'.\n", c));
      }
    }
    if (!unlinkedCharacters.isEmpty()) {
      for (String c : unlinkedCharacters) {
        errorMessage.append(String.format("Unlinked character '%s'.\n", c));
      }
    }
    if (!duplicatePlayerIds.isEmpty()) {
      for (long playerId : duplicatePlayerIds.keySet()) {
        errorMessage.append(String.format("Duplicate characters for player: '%s'.\n",
                                          Joiner.on(", ").join(duplicatePlayerIds.get(playerId))));
      }
    }

    if (errorMessage.length() > 0) {
      context.reply("Operation failed:\n" + errorMessage.toString());
      return;
    }

    for (Map.Entry<Long, Long> entry : playerIds.entrySet()) {
      long playerId = entry.getKey();
      long characterId = entry.getValue();

      try (Statement q = tx.prepare(
            "UPDATE players "
          + "SET ep_earned = ep_earned + :ep, gp_earned = gp_earned + :gp, gp_net = gp_net + :gp, ep_net = ep_net + :ep "
          + "WHERE id = :player_id;")) {
        q.bind("gp", gpDelta);
        q.bind("ep", epDelta);
        q.bind("player_id", playerId);
        q.executeUpdate();
      }

      addLogEntry(tx,
                  playerId, characterId,
                  getSourcePlayerId(context),
                  event,
                  raidType,
                  lootId,
                  epDelta,
                  gpDelta,
                  note);
    }

    if (playerIds.size() == 1 && lootId != -1) {
      long playerId = Iterables.getFirst(playerIds.keySet(), -1L);
      sendLootAwardMessage(context, tx, playerId, playerIds.get(playerId), lootId, epDelta, gpDelta);
    }
    else {
      context.reply(String.format("Operation successful - %d players updated.", playerIds.size()));
    }
  }

  private void sendLootAwardMessage(
      CommandContext context,
      Transaction tx,
      Long playerId,
      Long characterId,
      Long lootId,
      long epDelta,
      long gpDelta) throws Exception {
    PlayerId player = getPlayerId(tx, playerId);
    String characterName = getCharacterName(tx, characterId);
    LootInfo loot = LootInfo.getByLootId(tx, lootId);

    if (loot.hasItemId() && epDelta == 0 && gpDelta >= 0) {
      String description = null;
      if (player.transportUserId != null) {
        description = String.format("Congratulations, <@%s>!", player.transportUserId);
      }

      context.source().raw()
        .sendFile(loot.getTooltipImage(), "item.png")
        .embed(new EmbedBuilder()
            .setAuthor("Loot Awarded")
            .setTitle(loot.name, loot.getDatabaseURL())
            .addField("GP", String.format("+%d", gpDelta), true)
            .addField("Player", player.name, true)
            .addField("Character", characterName, true)
            .setDescription(description)
            .setImage("attachment://item.png")
            .setFooter(String.format("By Officer: %s", context.user().playerName()), null)
            .setColor(ItemRarity.values()[loot.itemRarity].color)
            .build())
        .queue();
    }
    else {
      context.reply("Operation successful - 1 players updated.");
    }
  }


  protected void addLogEntry(Transaction tx,
                             long targetPlayerId,
                             long targetCharacterId,
                             long sourcePlayerId,
                             EPGPEventType event,
                             RaidType raidType,
                             long lootId,
                             long epDelta,
                             long gpDelta,
                             String note) throws Exception {
    try (Statement q = tx.prepare("INSERT INTO epgp_log ("
        + "timestamp, target_player_id, target_character_id, source_player_id, type, raid_type, loot_id, ep_delta, gp_delta, note"
        + ") VALUES ("
        + ":timestamp, :target_player_id, :target_character_id, :source_player_id, :type, :raid_type, :loot_id, :ep_delta, :gp_delta, :note"
        + ");")) {
      q.bind("timestamp", Instant.now().getEpochSecond());
      q.bind("target_player_id", targetPlayerId);

      if (targetCharacterId < 0) {
        q.bind("target_character_id", SQLNull.INTEGER);
      } else {
        q.bind("target_character_id", targetCharacterId);
      }

      q.bind("source_player_id", sourcePlayerId);
      q.bind("type", event.ordinal());

      if (raidType == null) {
        q.bind("raid_type", SQLNull.INTEGER);
      } else {
        q.bind("raid_type", raidType.ordinal());
      }

      if (lootId < 0) {
        q.bind("loot_id", SQLNull.INTEGER);
      } else {
        q.bind("loot_id", lootId);
      }

      q.bind("ep_delta", epDelta);
      q.bind("gp_delta", gpDelta);

      if (note == null) {
        q.bind("note", SQLNull.TEXT);
      } else {
        q.bind("note", note);
      }

      q.executeInsert();
    }
  }

  protected void addDecayLogEntry(Transaction tx,
                                 long targetPlayerId,
                                 long sourcePlayerId,
                                 long epDelta,
                                 long gpDelta) throws Exception {
    addLogEntry(tx,
                targetPlayerId,
                -1,
                sourcePlayerId,
                EPGPEventType.DECAY,
                null,
                -1,
                epDelta,
                gpDelta,
                null);
  }

  public static String formatDate(long timestamp) {
    if (timestamp < 0) {
      return "Never";
    }
    Date date = new Date(timestamp * 1000);
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy h:mm a z", Locale.ENGLISH);
    sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
    return sdf.format(date);
  }

  protected static final String LOG_QUERY_FMT =
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
    + "LIMIT %d;";

  protected static class EPGPLogEntry {
    public long timestamp;
    public String targetPlayerName;
    public Optional<String> targetCharacterName;

    public EPGPEventType eventType;
    public String sourcePlayerName;
    public long ep;
    public long gp;
    public Optional<String> note;
    public Optional<String> lootName;
    public Optional<RaidType> raidType;

    public static EPGPLogEntry read(Cursor r) throws Exception {
      EPGPLogEntry entry = new EPGPLogEntry();
      entry.timestamp = r.get("timestamp", Long.class);
      entry.eventType = EPGPEventType.values()[r.get("type", Integer.class)];
      entry.sourcePlayerName = r.get("source_player_name", String.class);
      entry.targetPlayerName = r.get("target_player_name", String.class);
      entry.ep = r.get("ep_delta", Long.class);
      entry.gp = r.get("gp_delta", Long.class);
      entry.targetCharacterName = r.getNullable("target_character_name", String.class);
      entry.note = r.getNullable("note", String.class);
      entry.lootName = r.getNullable("loot_name", String.class);
      entry.raidType = r.getNullable("raid_type", Integer.class).map((i) -> RaidType.values()[i]);
      return entry;
    }

    public String getFormattedDate() {
      return formatDate(timestamp);
    }

    public boolean canCollapseUpInto(EPGPLogEntry other) {
      if (other.eventType == EPGPEventType.DECAY &&
          this.eventType == EPGPEventType.DECAY &&
          this.sourcePlayerName.equals(other.sourcePlayerName) &&
          Math.abs(other.timestamp - this.timestamp) < 600) {
        return true;
      }
      return (other.ep == this.ep) &&
             (other.gp == this.gp) &&
             (other.note.equals(this.note)) &&
             (other.raidType.equals(this.raidType)) &&
             (other.lootName.equals(this.lootName)) &&
             (other.eventType == this.eventType) &&
             (other.sourcePlayerName.equals(this.sourcePlayerName)) &&
             Math.abs(other.timestamp - this.timestamp) < 60 * 60;
    }

    public boolean isUndo(EPGPLogEntry other) {
      return (other.targetCharacterName.equals(this.targetCharacterName)) &&
             (other.targetPlayerName.equals(this.targetPlayerName)) &&
             (other.sourcePlayerName.equals(this.sourcePlayerName)) &&
             (other.eventType == this.eventType) &&
             (other.ep + this.ep == 0) &&
             (other.gp + this.gp == 0) &&
             (other.ep != 0 || other.gp != 0) &&
             (other.lootName.equals(this.lootName)) &&
             (other.raidType.equals(this.raidType)) &&
             (Math.abs(other.timestamp - this.timestamp) < 4 * 60 * 60);
    }
  }

  protected PageSource formatLogEntries(Cursor r, String title, Set<EPGPEventType> types, Set<String> loots, boolean showHidden) throws Exception {
    List<EPGPLogEntry> lastEntryList = null;
    List<List<EPGPLogEntry>> entries = new ArrayList<>();
    int offset = 0;

    while (r.next()) {
      EPGPLogEntry entry = EPGPLogEntry.read(r);
      if (!types.contains(entry.eventType)) {
        continue;
      }
      if (!loots.isEmpty()) {
        if (!entry.lootName.isPresent()) {
          continue;
        }
        if (!loots.contains(entry.lootName.get())) {
          continue;
        }
      }

      if (lastEntryList != null) {
        if (entry.canCollapseUpInto(lastEntryList.get(0)) && (lastEntryList.size() < 10 || lastEntryList.get(0).eventType == EPGPEventType.DECAY)) {
          lastEntryList.add(entry);
          continue;
        }

        EPGPLogEntry prev = lastEntryList.get(lastEntryList.size() - 1);
        if (entry.isUndo(prev)) {
          lastEntryList.remove(prev);
          if (lastEntryList.isEmpty()) {
            lastEntryList = null;
          }
          continue;
        }
      }

      lastEntryList = new ArrayList<>();
      lastEntryList.add(entry);
      entries.add(lastEntryList);
    }

    List<Map<String, Object>> data = new ArrayList<>();
    int entryIdx = 0;

    for (List<EPGPLogEntry> mergedEntry : entries) {
      if (mergedEntry.isEmpty()) {
        continue;
      }

      EPGPLogEntry first = mergedEntry.get(0);
      Map<String, Integer> targetPlayerNames = new HashMap<>();

      for (EPGPLogEntry entry : mergedEntry) {
        if (!targetPlayerNames.containsKey(entry.targetPlayerName)) {
          targetPlayerNames.put(entry.targetPlayerName, 0);
        }
        targetPlayerNames.put(entry.targetPlayerName, targetPlayerNames.get(entry.targetPlayerName) + 1);
      }


      Map<String, Object> row = new HashMap<>();
      List<String> playerNames = new ArrayList<>();
      for (Map.Entry<String, Integer> entry : targetPlayerNames.entrySet()) {
        if (entry.getValue() == 1) {
          playerNames.add(entry.getKey());
        } else {
          playerNames.add(String.format("%s (x%d)", entry.getKey(), entry.getValue()));
        }
      }

      row.put("date", first.getFormattedDate());
      row.put("officer", first.sourcePlayerName);
      row.put("player", Joiner.on(", ").join(playerNames));

      if (first.ep != 0) {
        if (first.ep > 0) {
          row.put("ep", String.format("+%d", first.ep));
        } else {
          row.put("ep", first.ep);
        }
      }

      if (first.gp != 0) {
        if (first.gp > 0) {
          row.put("gp", String.format("+%d", first.gp));
        } else {
          row.put("gp", first.gp);
        }
      }

      if (first.eventType == EPGPEventType.DECAY) {
        if (targetPlayerNames.size() > 1) {
          row.put("ep", "*");
          row.put("gp", "*");
          row.put("player", String.format("(%d players)", targetPlayerNames.size()));
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
        if (!showHidden) {
          continue;
        }
        row.put("reason", String.format("Hidden"));
      }

      else {
        row.put("reason", first.eventType);
      }

      if (entryIdx >= offset) {
        data.add(row);
      }
      entryIdx++;
    }

    return new TablePageSource(
        title,
        data,
        ImmutableList.of("date", "ep", "gp", "player", "reason", "officer"),
        ImmutableSet.of("ep", "gp"));
  }
}
