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
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TextTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public abstract class GuildAbstractCommandHandler extends CommandHandlerAbstract {
  protected void showOnlinePlayers(CommandContext context, Request request, boolean isOfficer)
      throws Exception {
    ArmoryRequest armoryRequest = new ArmoryRequest().setGetOnlineGuildMembers()
        .setReturnCharacters(true).setCallback((ArmoryResponse rsp) -> {
          try {
            formatOnlinePlayers(context, request, rsp, isOfficer);
          } catch (Exception e) {
            e.printStackTrace(System.err);
          }
        });

    if (!context.armory().offer(armoryRequest)) {
      context.reply("I am too busy to service your request at this time - try again later.");
    }
  }

  // TODO: Standardize display of character list.
  protected void formatOnlinePlayers(CommandContext context, Request request, ArmoryResponse rsp,
      boolean isOfficer) throws Exception {
    if (!rsp.didSucceed) {
      sendError(context, "Unable to connect to armory.");
    }

    String type = isOfficer ? "Officers" : "Members";

    List<Map<String, Object>> table = new ArrayList<>();

    try (Transaction tx = context.database().transaction()) {
      for (ArmoryAPI.Character c : rsp.characters) {
        try (Statement q = tx.prepare("SELECT c.*, p.name AS player_name " + "FROM characters AS c "
            + "LEFT JOIN players AS p ON p.id = c.player_id "
            + "WHERE lower(c.name) = :name AND c.deleted = 0;")) {
          q.bind("name", c.name.toLowerCase());
          try (Cursor r = q.executeFetch()) {
            if (r.next()) {
              // XXX: Could sync this from the guild roster, but the API doesn't send guild ranks.
              if (isOfficer && !context.config().officer_player_names
                  .contains(r.getNullable("player_name", String.class).orElse(""))) {
                continue;
              }

              Map<String, Object> row = new HashMap<>();
              row.put("player", r.getNullable("player_name", String.class));
              row.put("character", r.get("name", String.class));
              row.put("level", r.getNullable("level", Long.class));
              row.put("race", r.getNullable("race", String.class));
              row.put("class", r.getNullable("class", String.class));
              row.put("guild", r.getNullable("guild_name", String.class));
              row.put("spec1", r.getNullable("talent_spec1_tree", String.class));
              row.put("spec2", r.getNullable("talent_spec2_tree", String.class));
              table.add(row);
            } else {
              // TODO: Probably should just force a sync in the armory request so the row exists.
            }
          }
        }
      }
    }

    if (table.isEmpty()) {
      context.replyf("**Online %s (0):**\n*No matches found.\n", type);
      return;
    }

    String out = String.format("**Online %s (%d):**\n", type, table.size());
    if (!table.isEmpty()) {
      out += "```\n";
      out += TextTable.format(ImmutableList.of("player", "character", "level", "race", "class",
          "guild", "spec1", "spec2"), table, ImmutableSet.of());
      out += "```\n";
    }
    context.reply(out);
  }
}
