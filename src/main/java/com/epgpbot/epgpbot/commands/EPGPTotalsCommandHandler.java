package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TablePageSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class EPGPTotalsCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    String field = "ep_earned";
    if (request.hasFlag("sort")) {
      if (request.flag("sort").size() != 1) {
        sendCorrectUsage(context);
        return;
      }

      switch (request.flag("sort").get(0).toLowerCase()) {
        case "ep":
          field = "ep_earned";
          break;
        case "gp":
          field = "gp_earned";
          break;
        case "priority":
          field = "priority_net";
          break;
        case "items":
          field = "items_won";
          break;
        default:
          sendCorrectUsage(context);
          return;
      }
    }

    int minEP = -1;
    if (request.hasFlag("min-ep")) {
      if (request.flag("min-ep").size() != 1) {
        sendCorrectUsage(context);
        return;
      }
      try {
        minEP = Integer.parseInt(request.flag("min-ep").get(0));
      } catch (NumberFormatException e) {
        sendCorrectUsage(context);
        return;
      }
    }

    List<Map<String, Object>> data = new ArrayList<>();

    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare(
            "SELECT name, ep_earned, gp_earned, ep_net, gp_net, (ep_net / (:base_gp + gp_net)) AS priority_net, "
          + "((SELECT COUNT(*) FROM epgp_log WHERE target_player_id = players.id AND type = :loot_type AND gp_delta > 0) - (SELECT COUNT(*) FROM epgp_log WHERE target_player_id = players.id AND type = :loot_type AND gp_delta < 0)) AS items_won "
          + "FROM players "
          + "WHERE (ep_earned > 0 OR gp_earned > 0) "
          + "ORDER BY " + field + " DESC;")) {
        q.bind("loot_type", EPGPEventType.LOOT.ordinal());
        q.bind("base_gp", (double)context.epgp().baseGP());
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            if (minEP > 0 && r.get("ep_earned", Long.class) < minEP) {
              continue;
            }

            Map<String, Object> row = new HashMap<>();
            row.put("name", r.get("name", String.class));
            row.put("ep_earned", r.get("ep_earned", Long.class));
            row.put("ep_net", r.get("ep_net", Long.class));
            row.put("gp_earned", r.get("gp_earned", Long.class));
            row.put("gp_net", r.get("gp_net", Long.class) + context.epgp().baseGP());
            row.put("priority_net", String.format("%.3f", r.get("priority_net", Double.class)));
            row.put("items_won", r.get("items_won", Long.class));
            data.add(row);
          }
        }
      }
    }

    final String sortField = field;
    data.sort(new Comparator<Map<String, Object>>() {
      @Override
      public int compare(Map<String, Object> _a, Map<String, Object> _b) {
        Object a = _a.get(sortField);
        Object b = _b.get(sortField);

        if (a instanceof Long) {
          double aPriority = (Long)a;
          double bPriority = (Long)b;
          if (aPriority == bPriority) {
            return 0;
          }
          return aPriority < bPriority ? 1 : -1;
        }

        if (a instanceof Double) {
          double aPriority = (Double)a;
          double bPriority = (Double)b;
          if (aPriority == bPriority) {
            return 0;
          }
          return aPriority < bPriority ? 1 : -1;
        }

        return 0;

      }
    });

    context.replyWithPages(
        new TablePageSource(
            "EPGP Totals",
            data,
            ImmutableList.of("name", "ep_earned", "ep_net", "gp_earned", "gp_net", "items_won", "priority_net"),
            ImmutableSet.of("ep_earned", "ep_net", "gp_earned", "gp_net", "priority_net")));
  }

  @Override
  public String help() {
    return "[--sort <'EP'|'GP'|'ITEMS'|'PRIORITY'(default)>] - Display EPGP standings with total EPGP earned.";
  }

  @Override
  public String command() {
    return "epgp.totals";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
