package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class PlayerAddCommandHandler extends CommandHandlerAbstract {
  // TODO: Audit logging.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() != 1) {
      sendCorrectUsage(context);
      return;
    }

    String name = request.arguments().get(0);

    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare("SELECT id FROM players WHERE name = :name;")) {
        q.bind("name", name);
        try (Cursor r = q.executeFetch()) {
          if (r.next()) {
            context.reply("Operation failed - player with given name already exists.");
            return;
          }
        }
      }

      try (Statement q = tx.prepare("INSERT INTO players (name) VALUES (:name);")) {
        q.bind("name", name);
        q.executeInsert();
      }
    }

    context.reply("Operation successful.");
  }

  @Override
  public String help() {
    return "<player:string> - Adds a new player.";
  }

  @Override
  public String command() {
    return "player.add";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
