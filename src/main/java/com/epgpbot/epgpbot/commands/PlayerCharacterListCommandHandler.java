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
import com.epgpbot.util.TextTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class PlayerCharacterListCommandHandler extends CommandHandlerAbstract {
  // TODO: Standardize display of character list.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() != 1) {
      sendCorrectUsage(context);
      return;
    }

    String playerName = ucfirst(request.arguments().get(0));
    List<Map<String, Object>> table = new ArrayList<>();

    try (Transaction tx = context.database().transaction()) {
      PlayerId player = getPlayerId(tx, playerName);
      if (player == null) {
        sendError(context, "Unknown player '%s'.", playerName);
        return;
      }

      try (Statement q =
          tx.prepare("SELECT * FROM characters WHERE player_id = :player_id AND deleted = 0;")) {
        q.bind("player_id", player.id);
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

      context.reply("**Characters for __" + playerName + "__:**\n```\n"
          + TextTable.format(
              ImmutableList.of("name", "level", "race", "class", "guild", "spec1", "spec2"), table,
              ImmutableSet.of())
          + "```\n");
    }
  }

  @Override
  public String help() {
    return "<player:string> - Lists all of a player's characters.";
  }

  @Override
  public String command() {
    return "player.character.list";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
