package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class ItemMergeCommandHandler extends CommandHandlerAbstract {
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
    sendError(context, "Unknown loot '%s'.", name);
    return null;
  }

  // TODO: Audit logging.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() < 2) {
      sendCorrectUsage(context);
      return;
    }

    try (Transaction tx = context.database().transaction()) {
      Long id1 = getLootId(context, request.arguments().get(0));
      Long id2 = getLootId(context, request.arguments().get(1));

      if (id1 == null || id2 == null) {
        return;
      }
      if (id1 == id2) {
        sendCorrectUsage(context);
        return;
      }

      try (Statement q =
          tx.prepare("UPDATE epgp_log SET loot_id = :new_id WHERE loot_id = :old_id;")) {
        q.bind("old_id", id2);
        q.bind("new_id", id1);
        q.executeUpdate();
      }

      try (Statement q = tx.prepare("DELETE FROM loot WHERE id = :old_id;")) {
        q.bind("old_id", id2);
        q.executeUpdate();
      }
    }

    context.replyf("Operation successful.");
  }

  @Override
  public String help() {
    return "<item:string> <item-alt-name:string> - Merges two items into a single entry.";
  }

  @Override
  public String command() {
    return "item.merge";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.AUDIT_EPGP);
  }
}
