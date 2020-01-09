package com.epgpbot.epgpbot.commands;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.Argument;
import com.epgpbot.util.PlayerId;
import com.google.common.collect.ImmutableList;

public class EPGPLootBackfillCommandHandler extends AbstractEPGPCommandHandler {
  public Set<Long> getReceivedItems(Transaction tx, long playerId) throws Exception {
    Set<Long> result = new HashSet<>();

    try (Statement q = tx.prepare("SELECT * FROM epgp_log WHERE target_player_id = :player_id AND type = :type AND undoes IS NULL and undone_by IS NULL;")) {
      q.bind("player_id", playerId);
      q.bind("type", EPGPEventType.LOOT.ordinal());
      try (Cursor r = q.executeFetch()) {
        while(r.next()) {
          Optional<Long> lootId = r.getNullable("loot_id", Long.class);
          if (lootId.isPresent()) {
            result.add(lootId.get());
          }
        }
      }
    }

    return result;
  }

  protected void addLogEntry(
      Transaction tx,
      long sourcePlayerId,
      long targetPlayerId,
      long targetCharacterId,
      long lootId,
      long gpDelta,
      long timestamp) throws Exception {
    try (
      Statement q = tx.prepare(
            "INSERT INTO epgp_log ("
          + "timestamp, target_player_id, target_character_id, source_player_id, type, loot_id, gp_delta, action_timestamp"
          + ") VALUES ("
          + ":timestamp, :target_player_id, :target_character_id, :source_player_id, :type, :loot_id, :gp_delta, :action_timestamp"
          + ");")
    ) {
      q.bind("action_timestamp", Instant.now().getEpochSecond());
      q.bind("timestamp", timestamp);
      q.bind("target_player_id", targetPlayerId);
      q.bind("target_character_id", targetCharacterId);
      q.bind("source_player_id", sourcePlayerId);
      q.bind("type", EPGPEventType.LOOT.ordinal());
      q.bind("loot_id", lootId);
      q.bind("gp_delta", 1);
      q.executeInsert();
    }

    try (Statement q = tx.prepare(
        "UPDATE players "
      + "SET ep_earned = ep_earned + :ep, gp_earned = gp_earned + :gp, gp_net = gp_net + :gp, ep_net = ep_net + :ep "
      + "WHERE id = :player_id;")) {
      q.bind("gp", gpDelta);
      q.bind("ep", 0);
      q.bind("player_id", targetPlayerId);
      q.executeUpdate();
    }
  }

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    String message = "";

    try (Transaction tx = context.database().transaction()) {
      long sourcePlayerId = getSourcePlayerId(context);
      long gp = request.arg("gp", 0).longValue();
      PlayerId player = request.arg("character", 1).characterPlayerIdValue(tx);
      List<LootInfo> items = new ArrayList<>();
      for (Argument item : request.argsFrom("item", 2)) {
        items.add(item.lootValue(tx));
      }

      Set<Long> existingItems = getReceivedItems(tx, player.id);

      for (LootInfo item : items) {
        if (existingItems.contains(item.lootId)) {
          message += String.format("Skipped %s - already obtained.\n", item.name);
          continue;
        }
        message += String.format("Awarded %s.\n", item.name);
        addLogEntry(tx, sourcePlayerId, player.id, player.characterId, item.lootId, gp, 1);
      }
    }

    context.reply(message);
  }

  @Override
  public String help() {
    return "<gp:int> <character:string> <...item:string> - Awards item(s) to a player only if they have not already looted that item.";
  }

  @Override
  public String command() {
    return "epgp.loot.backfill";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
