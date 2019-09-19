package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class ItemAliasCommandHandler extends CommandHandlerAbstract {
  public Long getLootId(CommandContext context, String name) throws Exception {
    try (Transaction tx = context.database().transaction();
        Statement q = tx.prepare("SELECT id FROM loot WHERE lower(name) = :name;");) {
      q.bind("name", name);
      try (Cursor r = q.executeFetch()) {
        if (r.next()) {
          return r.get("id", Long.class);
        }
      }
    }
    return null;
  }

  public Long getAliasId(CommandContext context, String name) throws Exception {
    try (Transaction tx = context.database().transaction();
        Statement q = tx.prepare("SELECT id FROM loot_alias WHERE lower(name) = :name;");) {
      q.bind("name", name);
      try (Cursor r = q.executeFetch()) {
        if (r.next()) {
          return r.get("id", Long.class);
        }
      }
    }
    return null;
  }

  // TODO: Audit logging.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() < 2) {
      sendCorrectUsage(context);
      return;
    }

    String name = request.arguments().get(0);
    Long id = getLootId(context, name);
    if (id == null) {
      sendError(context, "Unknown loot '%s'.", name);
      return;
    }

    long errors = 0;

    for (String alias : request.argumentsFrom(1)) {
      if (getLootId(context, alias) != null) {
        sendError(context, "Alias '%s' conflicts with loot name.", alias);
        errors++;
      }
      if (getAliasId(context, alias) != null) {
        sendError(context, "Alias '%s' already exists.", alias);
        errors++;
      }
    }

    if (errors > 0) {
      return;
    }

    try (Transaction tx = context.database().transaction()) {
      for (String alias : request.argumentsFrom(1)) {
        try (Statement q =
            tx.prepare("INSERT INTO loot_alias (name, loot_id) VALUES (:name, :loot_id);")) {
          q.bind("name", alias);
          q.bind("loot_id", id);
          q.executeInsert();
        }
      }
    }

    context.reply("Operation successful.");
  }

  @Override
  public String help() {
    return "<item:string> <...alias:string> - Creates aliases for an item name.";
  }

  @Override
  public String command() {
    return "item.alias";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
