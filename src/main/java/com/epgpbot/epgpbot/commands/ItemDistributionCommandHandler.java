package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.epgpbot.schema.RaidType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.Argument;
import com.epgpbot.util.TablePageSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class ItemDistributionCommandHandler extends CommandHandlerAbstract {
  protected static final String QUERY =
      "SELECT \r\n" +
      "  pl.id, \r\n" +
      "  pl.name, \r\n" +
      "  IFNULL(tmp2.raid_clears,0) AS kills, \r\n" +
      "  IFNULL(tmp1.times_item_won,0) AS wins,\r\n" +
      "  (IFNULL(tmp2.raid_clears, 0) - IFNULL(tmp1.times_item_won, 0) * :raid_size) AS loot_priority\r\n" +
      "FROM players AS pl \r\n" +
      "LEFT JOIN (\r\n" +
      "    SELECT p.id, COUNT(l.id) as times_item_won\r\n" +
      "    FROM players as p \r\n" +
      "    JOIN epgp_log as l ON p.id = l.target_player_id\r\n" +
      "    JOIN loot as i ON i.id = l.loot_id\r\n" +
      "    WHERE i.id = :loot_id \r\n" +
      "    GROUP BY p.id\r\n" +
      ") as tmp1 ON tmp1.id = pl.id\r\n" +
      "LEFT JOIN (\r\n" +
      "  SELECT p.id, COUNT(l.id) as raid_clears\r\n" +
      "    FROM players as p \r\n" +
      "    JOIN epgp_log as l ON p.id = l.target_player_id\r\n" +
      "    WHERE l.raid_type = :raid_type\r\n" +
      "    GROUP BY p.id\r\n" +
      ") as tmp2 ON tmp2.id = pl.id \r\n" +
      "WHERE (tmp2.raid_clears IS NOT NULL OR tmp1.times_item_won IS NOT NULL) AND tmp2.raid_clears >= :min_raid_clears \r\n" +
      "ORDER BY loot_priority DESC;";

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    try (Transaction tx = context.database().transaction()) {
      List<Map<String, Object>> table = new ArrayList<>();
      int raidSize = request.flagArg("raid-size").intOption().orElse(40);
      int minClears = request.flagArg("min-clears").intOption().orElse(0);
      LootInfo loot = request.arg("item", 0).lootValue(tx);
      int raidType = request.arg("raid", 1).enumValue(RaidType.class).ordinal();
      Set<Long> includePlayerIds = new HashSet<>();
      for (Argument arg : request.argsFrom("character", 2)) {
        includePlayerIds.add(arg.characterPlayerIdValue(tx).id);
      }

      try (Statement q = tx.prepare(QUERY)) {
        q.bind("raid_size", raidSize);
        q.bind("raid_type", raidType);
        q.bind("loot_id", loot.lootId);
        q.bind("min_raid_clears", minClears);
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            if (!includePlayerIds.isEmpty() &&
                !includePlayerIds.contains(r.get("id", Long.class))) {
              continue;
            }

            Map<String, Object> row = new HashMap<>();
            row.put("player", r.get("name", String.class));
            row.put("kills", r.get("kills", Long.class));
            row.put("wins", r.get("wins", Long.class));
            row.put("preference", r.get("loot_priority", Long.class));
            table.add(row);
          }
        }
        context.replyWithPages(
            new TablePageSource(
                String.format("Item Distribution (%s)", loot.name),
                table,
                ImmutableList.of("player", "kills", "wins", "preference"),
                ImmutableSet.of("kills", "wins", "preference"))
              .setPerPage(20));
      }
    }
  }

  @Override
  public String help() {
    return "<item:string> <raid:string> [<...character:string>] - Tool to help distribute a piece of loot evenly amongst players.";
  }

  @Override
  public String command() {
    return "item.distribution";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
