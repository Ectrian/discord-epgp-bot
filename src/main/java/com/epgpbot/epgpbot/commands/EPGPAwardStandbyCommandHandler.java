package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class EPGPAwardStandbyCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() < 3) {
      sendCorrectUsage(context);
      return;
    }

    long ep;
    try {
      ep = Long.parseLong(request.arguments().get(0));
    } catch (NumberFormatException e) {
      sendCorrectUsage(context);
      return;
    }

    List<String> toCheckForDuplicates = ImmutableList.of();
    if (request.hasFlag("skip")) {
      toCheckForDuplicates = request.flag("skip");
    }

    String raidName = request.arguments().get(1);
    List<String> characterNames = request.argumentsFrom(2);

    String note = null;
    if (request.hasFlag("note") && !request.flag("note").isEmpty()) {
      note = request.flag("note").get(0);
    }

    try (Transaction tx = context.database().transaction()) {
      performEPGPUpdate(context, tx, EPGPEventType.STANDBY, ep, 0, null, raidName, note, characterNames, toCheckForDuplicates, false);
    }
  }

  @Override
  public String help() {
    return "<ep:int> <raid:string> <...character:string> [--note <:string>] [--skip <...character:string>] - Awards EP for standing by for a raid.";
  }

  @Override
  public String command() {
    return "epgp.award.standby";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
