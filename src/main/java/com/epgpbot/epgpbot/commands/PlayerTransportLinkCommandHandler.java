package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.transport.User;
import com.epgpbot.util.PlayerId;
import com.google.common.collect.ImmutableList;

public class PlayerTransportLinkCommandHandler extends CommandHandlerAbstract {
  // TODO: Audit logging.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    boolean force = request.hasFlag("force");

    try (Transaction tx = context.database().transaction()) {
      final PlayerId player = request.arg("player", 0).playerIdValue(tx);
      final User user = request.arg("user", 1).userValue(context);

      if (!force) {
        try (Statement q = tx.prepare(
            "SELECT * FROM transport_users WHERE id = :id;")) {
          q.bind("id", user.transportUserId());
          try(Cursor c = q.executeFetch()) {
            while (c.next()) {
              context.abort("Provided user is already linked to a player.");
            }
          }
        }

        try (Statement q = tx.prepare(
            "SELECT * FROM transport_users WHERE player_id = :player_id;")) {
          q.bind("player_id", player.id);
          try(Cursor c = q.executeFetch()) {
            while (c.next()) {
              context.abort("Provided player is already linked to another user.");
            }
          }
        }
      }

      try (Statement q = tx.prepare(
          "REPLACE INTO transport_users (id, player_id, name) VALUES (:id, :player_id, :name);")) {
        q.bind("id", user.transportUserId());
        q.bind("player_id", player.id);
        q.bind("name", user.transportUserName());
        q.executeInsert();
      }
    }

    context.reply("Operation successful.");
  }

  @Override
  public String help() {
    return "<player:string> <user:@ref> - Links a player to a Discord account.";
  }

  @Override
  public String command() {
    return "player.transport.link";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_PERMISSIONS);
  }
}
