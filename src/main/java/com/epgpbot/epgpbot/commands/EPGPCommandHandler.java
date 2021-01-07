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
import com.epgpbot.util.PlayerId;
import com.epgpbot.util.TextTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class EPGPCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> data = new ArrayList<>();
    String playerName = null;

    if (request.arguments().isEmpty()) {
      try (Transaction tx = context.database().transaction()) {
        PlayerId player = getInferredPlayer(tx, context);
        if (player == null) {
          sendError(context,
                "I'm not sure who you are. "
              + "Try using '!epgp <character>' instead, or ask an officer to link your Discord account via "
              + String.format("'!player.transport.link <player> %s'.",
                  context.user().qualifiedTransportUserName()));
          return;
        }

        try (Statement q = tx.prepare(
              "SELECT p.name, p.ep_net, p.gp_net, (p.ep_net / (:base_gp + p.gp_net)) AS priority "
            + "FROM players AS p "
            + "WHERE p.id = :id;")) {
          q.bind("base_gp", (double)context.epgp().baseGP());
          q.bind("id", player.id);
          try (Cursor r = q.executeFetch()) {
            if (r.next()) {
              Map<String, Object> row = new HashMap<>();
              playerName = r.get("name", String.class);
              row.put("ep", r.get("ep_net", Long.class));
              row.put("gp", r.get("gp_net", Long.class) + context.epgp().baseGP());
              row.put("priority", String.format("%.3f", r.get("priority", Double.class)));
              data.add(row);
            }
          }
        }
      }
    } else {
      try (Transaction tx = context.database().transaction()) {
        String characterName = ucfirst(request.arguments().get(0));
        try (Statement q = tx.prepare(
              "SELECT p.name, p.ep_net, p.gp_net, (p.ep_net / (:base_gp + p.gp_net)) AS priority "
            + "FROM players AS p "
            + "JOIN characters AS c ON c.player_id = p.id "
            + "WHERE lower(c.name) = :character_name;")) {
          q.bind("base_gp", (double)context.epgp().baseGP());
          q.bind("character_name", characterName.toLowerCase());
          try (Cursor r = q.executeFetch()) {
            if (r.next()) {
              Map<String, Object> row = new HashMap<>();
              playerName = r.get("name", String.class);
              row.put("ep", r.get("ep_net", Long.class));
              row.put("gp", r.get("gp_net", Long.class) + context.epgp().baseGP());
              row.put("priority", String.format("%.3f", r.get("priority", Double.class)));
              data.add(row);
            } else {
              sendError(context, "Unknown or unlinked character '%s'", characterName);
              return;
            }
          }
        }
      }
    }

    String out = String.format("**EPGP for __%s__:**\n", playerName);

    out += "```\n";
    out += TextTable.format(
      ImmutableList.of("ep", "gp", "priority"),
      data,
      ImmutableSet.of("ep", "gp", "priority")
    );
    out += "```\n";

    context.reply(out);
  }

  @Override
  public String help() {
    return "[<character:string>] - Displays EPGP for a character.";
  }

  @Override
  public String command() {
    return "epgp";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
