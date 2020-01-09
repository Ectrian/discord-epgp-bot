package com.epgpbot.epgpbot.commands;

import java.util.List;

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
    if (request.arguments().isEmpty() || request.mentions().isEmpty()) {
      sendCorrectUsage(context);
      return;
    }

    final String playerName = request.arguments().get(0);

    try (Transaction tx = context.database().transaction()) {
      final PlayerId player = getPlayerId(tx, playerName);
      if (player == null) {
        sendError(context, "Unknown player '%s'.", playerName);
        return;
      }

      for (User user : request.mentions()) {
        // TODO: Error if transport user is already linked to another player.
        // TODO: Warn if player is already linked to another transport user.
        try (Statement q = tx.prepare(
            "REPLACE INTO transport_users (id, player_id, name) VALUES (:id, :player_id, :name);")) {
          q.bind("id", user.transportUserId());
          q.bind("player_id", player.id);
          q.bind("name", user.transportUserName());
          q.executeInsert();
        }
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
