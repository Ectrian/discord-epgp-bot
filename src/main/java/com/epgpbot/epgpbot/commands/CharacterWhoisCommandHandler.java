package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epgpbot.armory.transport.ArmoryRequest;
import com.epgpbot.armory.transport.ArmoryResponse;
import com.epgpbot.config.ArmoryType;
import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TextTable;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class CharacterWhoisCommandHandler extends CommandHandlerAbstract {
  // TODO: Standardize display of character list.
  public void showCharacters(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> table = new ArrayList<>();
    List<String> invalidCharacters = new ArrayList<>();

    try (Transaction tx = context.database().transaction()) {
      for (String character : request.arguments()) {
        try (Statement q = tx.prepare(
              "SELECT c.*, p.name AS player_name "
            + "FROM characters AS c "
            + "LEFT JOIN players AS p ON p.id = c.player_id "
            + "WHERE lower(c.name) = :name AND c.deleted = 0;")) {
          q.bind("name", character.toLowerCase());
          try (Cursor r = q.executeFetch()) {
            if (r.next()) {
              Map<String, Object> row = new HashMap<>();
              row.put("player", r.getNullable("player_name", String.class).orElse(""));
              row.put("name", r.get("name", String.class));
              row.put("level", r.getNullable("level", Long.class));
              row.put("race", r.getNullable("race", String.class));
              row.put("class", r.getNullable("class", String.class));
              row.put("guild", r.getNullable("guild_name", String.class));
              row.put("spec1", r.getNullable("talent_spec1_tree", String.class));
              row.put("spec2", r.getNullable("talent_spec2_tree", String.class));
              // TODO: Gear Score?
              table.add(row);
            } else {
              invalidCharacters.add(ucfirst(character));
            }
          }
        }
      }
    }

    String out = String.format("**Results (%d):**\n", table.size());
    if (!table.isEmpty()) {
      out += "```\n";
      out += TextTable.format(
          ImmutableList.of("player", "name", "level", "race", "class", "guild", "spec1", "spec2"),
          table,
          ImmutableSet.of()
      );
      out += "```\n";
    }
    if (!invalidCharacters.isEmpty()) {
      out += "*Not Found:* " + Joiner.on(", ").join(invalidCharacters);
    }
    context.reply(out);
  }

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().isEmpty()) {
      sendCorrectUsage(context);
      return;
    }

    if (context.user().hasPermission(PermissionType.MODIFY_EPGP) &&
        context.config().game_armory_type != ArmoryType.NONE &&
        !request.hasFlag("no-sync") &&
        request.arguments().size() == 1) {
      ArmoryRequest armoryRequest = new ArmoryRequest()
          .addCharacters(request.arguments())
          .setCallback((ArmoryResponse rsp) -> {
            try {
              showCharacters(context, request);
            } catch (Exception e) {
              e.printStackTrace(System.err);
            }
          });

      if (context.armory().offer(armoryRequest)) {
        return;
      }
    }

    showCharacters(context, request);
  }

  @Override
  public String help() {
    return "<...character:string> [--no-sync] - Returns information about a character and any associated player.";
  }

  @Override
  public String command() {
    return "character.whois";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }

}
