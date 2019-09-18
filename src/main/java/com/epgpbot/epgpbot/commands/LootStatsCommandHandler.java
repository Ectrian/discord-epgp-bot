package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

public class LootStatsCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() != 1) {
      sendCorrectUsage(context);
      return;
    }

    try (Transaction tx = context.database().transaction()) {
      List<LootInfo> matches = LootInfo.search(tx, request.arguments().get(0));

      if (matches.isEmpty()) {
        sendError(context, "Unknown loot '%s'.", request.arguments().get(0));
        return;
      }

      if (matches.size() != 1 &&
          !matches.get(0).name.toLowerCase().equals(request.arguments().get(0).toLowerCase())) {
        sendError(context, "Ambiguous loot '%s'.", request.arguments().get(0));
        return;
      }

      LootInfo loot = matches.get(0);

      try (Statement q = tx.prepare(
            "SELECT "
          + "  MIN(timestamp) AS first_seen, "
          + "  MAX(timestamp) AS last_seen, "
          + "  COUNT(*) AS times_seen, "
          + "  SUM(gp_delta) AS gp_spent "
          + "FROM epgp_log "
          + "WHERE type = :type AND loot_id = :loot_id;")) {
        q.bind("loot_id", loot.lootId);
        q.bind("type", EPGPEventType.LOOT.ordinal());
        try (Cursor r = q.executeFetch()) {
          if (!r.next()) {
            return; // Always a result for aggregation.
          }

          // XXX: Drop rate.
          long firstSeen = r.getNullable("first_seen", Long.class).orElse(-1L);
          long lastSeen = r.getNullable("last_seen", Long.class).orElse(-1L);
          long timesSeen = r.getNullable("times_seen", Long.class).orElse(0L);
          long gpSpent = r.getNullable("gp_spent", Long.class).orElse(0L);

          MessageEmbed embed = new EmbedBuilder()
            .setAuthor("Loot Stats")
            .setTitle(loot.name, loot.getDatabaseURL())
            .setImage("attachment://item.png")
            .setColor(loot.getColor())
            .addField("First Seen", formatDate(firstSeen), false)
            .addField("Last Seen", formatDate(lastSeen), false)
            .addField("Times Seen", String.format("%d", timesSeen), false)
            .addField("Total GP Spent", String.format("%d", gpSpent), false)
            .addField("Average GP Cost",  String.format("%d", Math.round((double)gpSpent / timesSeen)), false)
            .build();

          if (loot.hasItemId()) {
            context.source().raw()
                .sendFile(loot.getTooltipImage(), "item.png")
                .embed(embed)
                .queue();
          }
          else {
            context.source().raw()
                .sendMessage(embed)
                .queue();
          }
        }
      }
    }
  }

  @Override
  public String help() {
    return "<item:string> - Displays stats about a piece of loot.";
  }

  @Override
  public String command() {
    return "item.stats";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
