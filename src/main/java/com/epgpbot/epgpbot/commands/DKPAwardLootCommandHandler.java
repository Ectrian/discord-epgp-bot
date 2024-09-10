package com.epgpbot.epgpbot.commands;

import java.util.List;
import java.util.Optional;

import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class DKPAwardLootCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() != 3) {
      sendCorrectUsage(context);
      return;
    }

    long ep = request.arg("ep", 0).longValue();
    String lootName = request.arg("item", 1).stringValue();
    String characterName = request.arg("character", 2).stringValue();
    String note = request.flagArg("note").stringOption().isPresent() ?
                  request.flagArg("note").stringValue() :
                  null;
    Optional<Long> time = request.flagArg("time").timeOption();
    boolean createLoot = request.hasFlag("add-item");

    try (Transaction tx = context.database().transaction()) {
      performEPGPUpdate(
          context, tx, EPGPEventType.LOOT, -ep,
          0, lootName, null, note,
          ImmutableList.of(characterName),
          ImmutableList.of(), createLoot, time);
    }
  }

  @Override
  public String help() {
    return "<ep:int> <item:string> <character:string> [--note <note:string>] [--time <time:datetime>] [--add-item] - Deducts EP for looting an item.";
  }

  @Override
  public String command() {
    return "dkp.award.loot";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
