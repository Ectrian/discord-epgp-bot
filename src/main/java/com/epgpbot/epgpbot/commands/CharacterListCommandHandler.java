package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TablePageSource;
import com.google.common.collect.ImmutableList;

public class CharacterListCommandHandler extends CommandHandlerAbstract {
  // TODO: Standardize display of character list.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> table = new ArrayList<>();

    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare("SELECT * FROM characters WHERE deleted = 0;")) {
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", r.get("name", String.class));
            row.put("level", r.getNullable("level", Long.class));
            row.put("race", r.getNullable("race", String.class));
            row.put("class", r.getNullable("class", String.class));
            row.put("guild", r.getNullable("guild_name", String.class));
            row.put("spec1", r.getNullable("talent_spec1_tree", String.class));
            row.put("spec2", r.getNullable("talent_spec2_tree", String.class));
            table.add(row);
          }
        }
      }
    }

    context.replyWithPages(new TablePageSource("Characters", table, ImmutableList.of("name", "level", "race", "class", "guild", "spec1", "spec2")));
  }

  @Override
  public String help() {
    return "- Lists all characters in the database.";
  }

  @Override
  public String command() {
    return "character.list";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.ARMORY_SYNC);
  }

}
