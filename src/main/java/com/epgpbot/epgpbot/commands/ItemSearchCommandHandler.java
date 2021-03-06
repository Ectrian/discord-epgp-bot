package com.epgpbot.epgpbot.commands;

import java.util.List;
import java.util.stream.Collectors;

import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.EmbedLinkListPageSource;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class ItemSearchCommandHandler extends CommandHandlerAbstract {
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

    List<String> links = matches.stream()
        .map((li) -> String.format("[%s](%s)", li.name, li.getDatabaseURL()))
        .collect(Collectors.toList());

    context.replyWithPages(new EmbedLinkListPageSource(
        "Search Results",
        LootInfo.getDatabaseSearchURL(query),
        links));
  }

  @Override
  public String help() {
    return "<item:string> - Searches for items.";
  }

  @Override
  public String command() {
    return "item.search";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
