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

public class InvinciblesStandingCommandHandler extends CommandHandlerAbstract {
  protected static final String INVINCIBLES_QUERY =
      "SELECT \r\n" +
      "  pl.id, \r\n" +
      "  pl.name, \r\n" +
      "  IFNULL(tmp2.lod_kills,0) AS kills, \r\n" +
      "  IFNULL(tmp1.invincibles_won,0) AS invincibles,\r\n" +
      "  (IFNULL(tmp2.lod_kills,0) - IFNULL(tmp1.invincibles_won,0) * 25) AS loot_priority\r\n" +
      "FROM players AS pl \r\n" +
      "LEFT JOIN (\r\n" +
      "    SELECT p.id, COUNT(l.id) as invincibles_won\r\n" +
      "    FROM players as p \r\n" +
      "    JOIN epgp_log as l ON p.id = l.target_player_id\r\n" +
      "    JOIN loot as i ON i.id = l.loot_id\r\n" +
      "  WHERE i.name = 'Invincible' \r\n" +
      "    GROUP BY p.id\r\n" +
      ") as tmp1 ON tmp1.id = pl.id\r\n" +
      "LEFT JOIN (\r\n" +
      "  SELECT p.id, COUNT(l.id) as lod_kills\r\n" +
      "    FROM players as p \r\n" +
      "    JOIN epgp_log as l ON p.id = l.target_player_id\r\n" +
      "    WHERE l.raid_type = 10\r\n" +
      "    GROUP BY p.id\r\n" +
      ") as tmp2 ON tmp2.id = pl.id \r\n" +
      "WHERE (tmp2.lod_kills IS NOT NULL OR tmp1.invincibles_won IS NOT NULL) AND tmp2.lod_kills >= :min_lod_kills \r\n" +
      "ORDER BY loot_priority DESC;";

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> table = new ArrayList<>();

    try (
      Transaction tx = context.database().transaction();
      Statement q = tx.prepare(INVINCIBLES_QUERY);
    ) {
      q.bind("min_lod_kills", request.hasFlag("all") ? 0 : 5);  // TODO: Configurable?
      try (Cursor r = q.executeFetch()) {
        while (r.next()) {
          Map<String, Object> row = new HashMap<>();
          row.put("player", r.get("name", String.class));
          row.put("lod kills", r.get("kills", Long.class));
          row.put("invincibles", r.get("invincibles", Long.class));
          row.put("preference", r.get("loot_priority", Long.class));
          table.add(row);
        }
      }
    }

    context.replyWithPages(
        new TablePageSource(
            "Invincibles",
            table,
            ImmutableList.of("player", "lod kills", "invincibles", "preference"),
            ImmutableSet.of("invincibles", "lod kills", "preference"))
          .setPerPage(20));
  }

  @Override
  public String help() {
    return "[--all] - Displays information about Invincibles.";
  }

  @Override
  public String command() {
    return "invincible.standings";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
