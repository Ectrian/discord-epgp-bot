package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
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

public class EPGPTopDockedCommandHandler extends AbstractEPGPCommandHandler {
  private static final String TOP_PENALTIES_QUERY = "" +
      "SELECT \r\n" +
      "    target_player_id,\r\n" +
      "  p.name,\r\n" +
      "    SUM(ep_delta) as total_ep_delta,\r\n" +
      "    SUM(gp_delta) as total_gp_delta,\r\n" +
      "    SUM(CASE WHEN ep_delta < 0 THEN 1 ELSE 0 END) + SUM(CASE WHEN gp_delta < 0 THEN 1 ELSE 0 END) - SUM(CASE WHEN ep_delta > 0 THEN 1 ELSE 0 END) - SUM(CASE WHEN gp_delta > 0 THEN 1 ELSE 0 END) as times_docked\r\n" +
      "FROM epgp_log \r\n" +
      "JOIN players AS p ON p.id = target_player_id\r\n" +
      "WHERE type = :type\r\n" +
      "GROUP BY target_player_id\r\n" +
      "ORDER BY times_docked DESC\r\n" +
      "LIMIT 500;";

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> data = new ArrayList<>();

    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare(TOP_PENALTIES_QUERY)) {
        q.bind("type", EPGPEventType.PENALTY.ordinal());
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            Map<String, Object> row = new HashMap<>();
            long epDelta = r.get("total_ep_delta", Long.class);
            long gpDelta = r.get("total_gp_delta", Long.class);
            if (epDelta == 0 && gpDelta == 0) {
              continue;
            }
            row.put("player", r.get("name", String.class));
            row.put("penalties", r.get("times_docked", Long.class));
            row.put("ep_delta", epDelta);
            row.put("gp_delta", gpDelta);
            data.add(row);
          }
        }
      }
    }

    context.replyWithPages(
        new TablePageSource(
            "EPGP Penalty Standings",
            data,
            ImmutableList.of("player", "penalties", "ep_delta", "gp_delta"),
            ImmutableSet.of("penalties", "ep_delta", "gp_delta")));
  }

  @Override
  public String help() {
    return " - Displays the most docked players.";
  }

  @Override
  public String command() {
    return "epgp.topdocked";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }

}
