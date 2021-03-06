package com.epgpbot.epgpbot.commands;

import java.util.List;
import java.util.Optional;

import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class EPGPAwardIncentiveCommandHandler extends AbstractEPGPCommandHandler {
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

    final String note = request.arguments().get(1);
    final List<String> characterNames = request.argumentsFrom(2);

    try (Transaction tx = context.database().transaction()) {
      performEPGPUpdate(context, tx, EPGPEventType.INCENTIVE, ep, 0, null, null, note, characterNames, ImmutableList.of(), false, Optional.empty());
    }
  }

  @Override
  public String help() {
    return "<ep:int> <reason:string> <...character:string> - Awards EP for non-raid reasons.";
  }

  @Override
  public String command() {
    return "epgp.award.incentive";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
