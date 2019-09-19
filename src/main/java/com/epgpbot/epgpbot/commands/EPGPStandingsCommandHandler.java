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
import com.google.common.collect.ImmutableSet;

public class EPGPStandingsCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> data = new ArrayList<>();

    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare("SELECT name, ep_net, gp_net, (ep_net / (:base_gp + gp_net)) AS priority FROM players WHERE (ep_net > 0 OR gp_net > 0) ORDER BY priority DESC, name ASC;")) {
        q.bind("base_gp", (double)context.epgp().baseGP());
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", r.get("name", String.class));
            row.put("ep", r.get("ep_net", Long.class));
            row.put("gp", r.get("gp_net", Long.class) + context.epgp().baseGP());
            row.put("priority", String.format("%.3f", r.get("priority", Double.class)));

            data.add(row);
          }
        }
      }
    }

    context.replyWithPages(
        new TablePageSource(
            "EPGP Standings",
            data,
            ImmutableList.of("name", "ep", "gp", "priority"),
            ImmutableSet.of("ep", "gp", "priority"))
          .setPerPage(40));
  }

  @Override
  public String help() {
    return "- Displays the current EPGP standings.";
  }

  @Override
  public String command() {
    return "epgp.standings";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
