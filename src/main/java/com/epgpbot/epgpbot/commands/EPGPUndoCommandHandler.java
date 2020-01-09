package com.epgpbot.epgpbot.commands;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.database.mysql.SQLNull;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

// XXX: Not entirely correct if something is undone after a decay occurs.
public class EPGPUndoCommandHandler extends AbstractEPGPCommandHandler {
  private long performUndo(CommandContext context, Transaction tx, Cursor r) throws Exception {
    long id = r.get("id", Long.class);
    long targetPlayerId = r.get("target_player_id", Long.class);
    long epDelta = -1 * r.get("ep_delta", Long.class);
    long gpDelta = -1 * r.get("gp_delta", Long.class);
    int type = r.get("type", Integer.class);
    Optional<Integer> raidType = r.getNullable("raid_type", Integer.class);
    Optional<Long> targetCharacterId = r.getNullable("target_character_id", Long.class);
    Optional<Long> lootId = r.getNullable("loot_id", Long.class);

    try (Statement q = tx.prepare(
          "INSERT INTO epgp_log ("
        + "    timestamp, "
        + "    action_timestamp, "
        + "    target_player_id, "
        + "    target_character_id, "
        + "    source_player_id, "
        + "    type,"
        + "    raid_type,"
        + "    loot_id,"
        + "    ep_delta,"
        + "    gp_delta,"
        + "    undoes"
        + ") VALUES ("
        + "    :timestamp, "
        + "    :timestamp, "
        + "    :target_player_id, "
        + "    :target_character_id, "
        + "    :source_player_id, "
        + "    :type,"
        + "    :raid_type,"
        + "    :loot_id,"
        + "    :ep_delta,"
        + "    :gp_delta,"
        + "    :undoes"
        + ");")) {

      q.bind("timestamp", Instant.now().getEpochSecond());
      q.bind("target_player_id", targetPlayerId);
      q.bind("source_player_id", getSourcePlayerId(context));
      q.bind("type", type);
      q.bind("undoes", id);
      q.bind("ep_delta", epDelta);
      q.bind("gp_delta", gpDelta);

      if (raidType.isPresent()) {
        q.bind("raid_type", raidType.get());
      } else {
        q.bind("raid_type", SQLNull.INTEGER);
      }

      if (lootId.isPresent()) {
        q.bind("loot_id", lootId.get());
      } else {
        q.bind("loot_id", SQLNull.INTEGER);
      }

      if (targetCharacterId.isPresent()) {
        q.bind("target_character_id", targetCharacterId.get());
      } else {
        q.bind("target_character_id", SQLNull.INTEGER);
      }

      long undoID = q.executeInsert();

      try (Statement q2 = tx.prepare(
            "UPDATE players "
          + "SET ep_earned = ep_earned + :ep, gp_earned = gp_earned + :gp, gp_net = gp_net + :gp, ep_net = ep_net + :ep "
          + "WHERE id = :player_id;")) {
        q2.bind("gp", gpDelta);
        q2.bind("ep", epDelta);
        q2.bind("player_id", targetPlayerId);
        q2.executeUpdate();
      }

      return undoID;
    }
  }

  public void handleUndo(CommandContext context, Transaction tx, long id) throws Exception {
    try (Statement q = tx.prepare("SELECT * FROM epgp_log WHERE id = :id;")) {
      q.bind("id", id);
      try (Cursor r = q.executeFetch()) {
        if (!r.next()) {
          context.abort("Invalid ID '%d'.", id);
        }

        Optional<Long> undoneBy = r.getNullable("undone_by", Long.class);
        if (undoneBy.isPresent()) {
          context.abort("Operation has already been undone. To undo an undo, simply repeat the original operation.");
        }

        Optional<Long> undoes = r.getNullable("undoes", Long.class);
        if (undoes.isPresent()) {
          context.abort("UNDO operations cannot be undone. To undo an undo, simply repeat the original operation.");
        }

        EPGPEventType eventType = EPGPEventType.values()[r.get("type", Integer.class)];
        if (eventType == EPGPEventType.DECAY) {
          context.abort("DECAY operations cannot be undone.");
        }

        long undoID = performUndo(context, tx, r);
        try (Statement q2 = tx.prepare("UPDATE epgp_log SET undone_by = :undo_id WHERE id = :id;")) {
          q2.bind("undo_id", undoID);
          q2.bind("id", id);
          q2.executeUpdate();
        }
      }
    }
  }

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    // XXX: Should show the undone log entry on success.
    List<Long> ids = new ArrayList<>();

    for (int i = 0; i < request.arguments().size(); i++) {
      Optional<Long> id = getLongArg(request, i);
      if (!id.isPresent()) {
        sendCorrectUsage(context);
        return;
      }
      ids.add(id.get());
    }

    if (ids.isEmpty()) {
      sendCorrectUsage(context);
      return;
    }

    try (Transaction tx = context.database().transaction()) {
      for (long id : ids) {
        handleUndo(context, tx, id);
      }
    }

    context.replyf("Operation successful.");
  }

  @Override
  public String help() {
    return "<...id:int> - Reverses an EPGP operation.";
  }

  @Override
  public String command() {
    return "epgp.undo";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
