package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.epgpbot.schema.game.ItemRarity;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import net.dv8tion.jda.core.EmbedBuilder;

public class ItemLinkCommandHandler extends CommandHandlerAbstract {
  private void sendItemLink(
      CommandContext context,
      LootInfo loot,
      String footer) throws Exception {
    context.source().raw()
      .sendFile(loot.getTooltipImage(), "item.png")
      .embed(new EmbedBuilder()
          .setTitle(loot.name, loot.getDatabaseURL())
          .setImage("attachment://item.png")
          .setFooter(footer, null)
          .setColor(ItemRarity.values()[loot.itemRarity].color)
          .build())
      .queue();
  }

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().isEmpty()) {
      sendCorrectUsage(context);
      return;
    }

    String query = Joiner.on(" ").join(request.arguments());
    List<LootInfo> matches;
    try (Transaction tx = context.database().transaction()) {
      matches = LootInfo.search(tx, query);
    }

    if (matches.isEmpty()) {
      sendError(context, "No items found matching '%s'.", query);
      return;
    }

    for (LootInfo match : matches) {
      String footer = null;
      if (matches.size() > 1 && !match.name.toLowerCase().equals(query.toLowerCase())) {
        footer = String.format("Note: %d additional matches for '%s' - try using a more exact name.",
            matches.size() - 1, query);
      }
      sendItemLink(context, match, footer);
      return;
    }
  }

  @Override
  public String help() {
    return "<id:int|name:string> - Displays an item tool-tip.";
  }

  @Override
  public String command() {
    return "item.link";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
