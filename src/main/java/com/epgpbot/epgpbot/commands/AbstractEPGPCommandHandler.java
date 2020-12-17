package com.epgpbot.epgpbot.commands;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.database.mysql.SQLNull;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.epgpbot.epgpbot.schema.RaidType;
import com.epgpbot.epgpbot.schema.game.ItemRarity;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.util.PlayerId;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import net.dv8tion.jda.api.EmbedBuilder;

public abstract class AbstractEPGPCommandHandler extends CommandHandlerAbstract {
  protected long getSourcePlayerId(CommandContext context) throws Exception {
    if (!context.user().hasPlayer()) {
      context.abort("You must link yourself to a player using !player.link before using this command.");
    }
    return context.user().playerId();
  }

  protected Long getLootId(Transaction tx, String name, boolean create) throws Exception {
    LootInfo info = LootInfo.searchForSingleMatch(tx, name);
    if (info != null) {
      return info.lootId;
    }

    if (!create) {
      return null;
    }

    try (Statement q = tx.prepare("INSERT INTO loot (name) VALUES (:name);")) {
      q.bind("name", name);
      return q.executeInsert();
    }
  }

  protected void performEPGPUpdate(CommandContext context, Transaction tx, EPGPEventType event,
                                   long epDelta, long gpDelta, String lootName, String raidName,
                                   String note, List<String> characters, List<String> charactersToCheckForDuplicates,
                                   boolean createLoot, Optional<Long> timestamp) throws Exception {
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
                  note,
                  timestamp);
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

      InputStream tooltip = loot.getTooltipImage();

      if (tooltip != null) {
        context.source().raw()
          .sendFile(tooltip, "item.png")
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
      } else {
        context.reply("Operation successful - 1 players updated.");
      }
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
                             String note,
                             Optional<Long> timestamp) throws Exception {
    try (Statement q = tx.prepare("INSERT INTO epgp_log ("
        + "timestamp, target_player_id, target_character_id, source_player_id, type, raid_type, loot_id, ep_delta, gp_delta, note, action_timestamp"
        + ") VALUES ("
        + ":timestamp, :target_player_id, :target_character_id, :source_player_id, :type, :raid_type, :loot_id, :ep_delta, :gp_delta, :note, :action_timestamp"
        + ");")) {
      long actionTimestamp = Instant.now().getEpochSecond();
      q.bind("action_timestamp", actionTimestamp);
      q.bind("timestamp", timestamp.orElse(actionTimestamp));
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
                null,
                Optional.empty());
  }
}
