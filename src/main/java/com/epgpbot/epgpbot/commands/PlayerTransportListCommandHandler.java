package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.List;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class PlayerTransportListCommandHandler extends CommandHandlerAbstract {

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() != 1) {
      sendCorrectUsage(context);
      return;
    }

    String playerName = request.arguments().get(0);
    List<String> matches = new ArrayList<>();
    PlayerId player;

    try (Transaction tx = context.database().transaction()) {
      player = getPlayerId(tx, playerName);
      if (player == null) {
        sendError(context, "Unknown player '%s'.", playerName);
        return;
      }

      try (Statement q = tx.prepare("SELECT * FROM transport_users WHERE player_id = :id;")) {
        q.bind("id", player.id);
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            matches.add(String.format("<@%s>", r.get("id", String.class)));
          }
        }
      }
    }

    if (matches.isEmpty()) {
      context.replyf("No linked accounts for '%s'.", player.name);
      return;
    }

    context.replyf("**Linked Accounts for %s:** %s", player.name, Joiner.on(", ").join(matches));
  }

  @Override
  public String help() {
    return "<player:string> - Lists a player's Discord accounts.";
  }

  @Override
  public String command() {
    return "player.transport.list";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }

}
