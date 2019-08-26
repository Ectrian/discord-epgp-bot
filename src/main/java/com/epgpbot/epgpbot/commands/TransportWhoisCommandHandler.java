package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.transport.User;
import com.google.common.collect.ImmutableList;

public class TransportWhoisCommandHandler extends CommandHandlerAbstract {

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.mentions().isEmpty()) {
      sendCorrectUsage(context);
      return;
    }

    for (User u : request.mentions()) {
      try (Transaction tx = context.database().transaction()) {
        try (Statement q = tx.prepare(
              "SELECT p.name "
            + "FROM players AS p "
            + "JOIN transport_users AS tu ON p.id = tu.player_id "
            + "WHERE tu.id = :id;")) {
          q.bind("id", u.transportUserId());
          try (Cursor r = q.executeFetch()) {
            if (r.next()) {
              String playerName = r.get("name", String.class);
              context.replyf("'@%s' belongs to player '%s'.\n", u.transportUserName(), playerName);
            } else {
              context.replyf("No player linked for '@%s'.\n", u.transportUserName());
            }
          }
        }
      }
    }
  }

  @Override
  public String help() {
    return "@mention - Displays the player name for a Discord user.";
  }

  @Override
  public String command() {
    return "transport.whois";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
