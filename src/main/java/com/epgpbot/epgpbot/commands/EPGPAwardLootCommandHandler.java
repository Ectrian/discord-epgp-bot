package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class EPGPAwardLootCommandHandler extends AbstractEPGPCommandHandler {
  // TODO: Use an items table database to validate loot against?
  // TODO: Add loot aliases (e.g. "DBW" -> "Deathbringer's Will")?
  // TODO: Merge existing loot entries w/ typos (add command for it).
  // TODO: Add loot stats command.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() < 3 || request.arguments().size() > 4) {
      sendCorrectUsage(context);
      return;
    }

    long gp;
    try {
      gp = Long.parseLong(request.arguments().get(0));
    } catch (NumberFormatException e) {
      sendCorrectUsage(context);
      return;
    }

    final String lootName = request.arguments().get(1);
    final String characterName = request.arguments().get(2);

    String note = null;
    if (request.arguments().size() >= 4) {
      note = request.arguments().get(3);
    }

    boolean createLoot = request.hasFlag("add-item");

    try (Transaction tx = context.database().transaction()) {
      performEPGPUpdate(context, tx, EPGPEventType.LOOT, 0, gp, lootName, null, note, ImmutableList.of(characterName), ImmutableList.of(), createLoot);
    }
  }

  @Override
  public String help() {
    return "<gp:int> <item:string> <character:string> [<note:string>] [--add-item] - Awards GP for looting an item.";
  }

  @Override
  public String command() {
    return "epgp.award.loot";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
