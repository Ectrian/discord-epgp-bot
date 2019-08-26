package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TextTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class EPGPCompareCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> data = new ArrayList<>();
    Set<String> missingCharacterNames = new HashSet<>();

    try (Transaction tx = context.database().transaction()) {
      for (String characterName : request.arguments()) {
        try (Statement q = tx.prepare(
              "SELECT p.name, p.ep_net, p.gp_net, p.ep_earned, p.gp_earned, (p.ep_net / (:base_gp + p.gp_net)) AS priority, "
            + "((SELECT COUNT(*) FROM epgp_log WHERE target_player_id = p.id AND type = :loot_type AND gp_delta > 0) - (SELECT COUNT(*) FROM epgp_log WHERE target_player_id = p.id AND type = :loot_type AND gp_delta < 0)) AS items_won "
            + "FROM players AS p "
            + "JOIN characters AS c ON c.player_id = p.id "
            + "WHERE lower(c.name) = :character_name "
            + "ORDER BY priority DESC, name ASC;")) {
          q.bind("base_gp", (double)context.epgp().baseGP());
          q.bind("character_name", characterName.toLowerCase());
          q.bind("loot_type", EPGPEventType.LOOT.ordinal());
          try (Cursor r = q.executeFetch()) {
            if (r.next()) {
              Map<String, Object> row = new HashMap<>();
              row.put("player", r.get("name", String.class));
              row.put("character", ucfirst(characterName));
              row.put("ep", r.get("ep_net", Long.class));
              row.put("gp", r.get("gp_net", Long.class) + context.epgp().baseGP());
              row.put("ep_earned", r.get("ep_earned", Long.class));
              row.put("gp_earned", r.get("gp_earned", Long.class));
              row.put("items_won", r.get("items_won", Long.class));
              row.put("priority", String.format("%.3f", r.get("priority", Double.class)));
              row.put("raw_priority", r.get("priority", Double.class));
              row.put("avg_item_cost", Math.round(r.get("gp_earned", Long.class) / (double)r.get("items_won", Long.class)));
              data.add(row);
            } else {
              missingCharacterNames.add(characterName);
            }
          }
        }
      }
    }

    data.sort(new Comparator<Map<String, Object>>() {
      @Override
      public int compare(Map<String, Object> a, Map<String, Object> b) {
        double aPriority = (Double)a.get("raw_priority");
        double bPriority = (Double)b.get("raw_priority");
        if (aPriority == bPriority) {
          return 0;
        }
        return aPriority < bPriority ? 1 : -1;
      }
    });

    String out = "**EPGP Comparison:**\n";

    List<String> fields = ImmutableList.of("player", "character", "ep", "gp", "priority");
    if (request.hasFlag("details")) {
      fields = ImmutableList.of("player", "character", "ep", "gp", "priority", "ep_earned", "gp_earned", "items_won", "avg_item_cost");
    }

    out += "```\n" + TextTable.format(
      fields,
      data,
      ImmutableSet.of("ep", "gp", "ep_earned", "gp_earned", "items_won", "priority", "avg_item_cost")
    ) + "```\n";

    for (String characterName : missingCharacterNames) {
      out += String.format("*Warning:* Skipped unknown or unlinked character '%s'.\n", characterName);
    }

    context.reply(out);
  }

  @Override
  public String help() {
    return "<...character:string> [--details] - Compares EPGP across multiple characters.";
  }

  @Override
  public String command() {
    return "epgp.compare";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
