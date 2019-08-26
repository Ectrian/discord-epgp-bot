package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epgpbot.armory.transport.ArmoryAPI;
import com.epgpbot.armory.transport.ArmoryRequest;
import com.epgpbot.armory.transport.ArmoryResponse;
import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TextTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class PlayerCreepCommandHandler extends CommandHandlerAbstract {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() < 1) {
      sendCorrectUsage(context);
    }

    PlayerId player;
    try (Transaction tx = context.database().transaction()) {
      player = getPlayerId(tx, request.arguments().get(0));
    }
    if (player == null) {
      sendError(context, "Unknown player '%s'.", ucfirst(request.arguments().get(0)));
      return;
    }

    List<String> characters = new ArrayList<>();

    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare("SELECT name FROM characters WHERE player_id = :player_id;")) {
        q.bind("player_id", player.id);
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            characters.add(r.get("name", String.class));
          }
        }
      }
    }

    ArmoryRequest armoryRequest = new ArmoryRequest().addCharacters(characters).setForced(true)
        .setReturnCharacters(true).setCallback((ArmoryResponse data) -> {
          try {
            showCharacters(context, request, player, data);
          } catch (Exception e) {
            e.printStackTrace(System.err);
          }
        });

    if (!context.armory().offer(armoryRequest)) {
      context.reply("I am too busy to service your request at this time - try again later.");
    }
  }

  // TODO: Standardize display of character list.
  private void showCharacters(CommandContext context, Request request, PlayerId player,
      ArmoryResponse data) throws Exception {
    List<Map<String, Object>> table = new ArrayList<>();

    try (Transaction tx = context.database().transaction()) {
      for (ArmoryAPI.Character character : data.characters) {
        if (!character.online) {
          continue;
        }
        try (Statement q = tx.prepare("SELECT c.*, p.name AS player_name " + "FROM characters AS c "
            + "LEFT JOIN players AS p ON p.id = c.player_id "
            + "WHERE lower(c.name) = :name AND c.deleted = 0;")) {
          q.bind("name", character.name.toLowerCase());
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
              row.put("online", character.online ? "YES" : "");
              table.add(row);
            }
          }
        }
      }
    }

    String out = String.format("**Online Characters for *%s* (%d of %d):**\n", player.name,
        table.size(), data.characters.size());
    if (!table.isEmpty()) {
      out += "```\n";
      out += TextTable.format(
          ImmutableList.of("name", "level", "race", "class", "guild", "spec1", "spec2", "online"),
          table, ImmutableSet.of());
      out += "```\n";
    } else {
      out += "No online characters found.\n";
    }
    context.reply(out);
  }

  @Override
  public String help() {
    return "<player:string> - Returns all of a player's online characters.";
  }

  @Override
  public String command() {
    return "player.character.online";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.ARMORY_SYNC);
  }
}
