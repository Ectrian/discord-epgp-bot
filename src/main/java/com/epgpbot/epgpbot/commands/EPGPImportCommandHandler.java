package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class EPGPImportCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() != 3) {
      sendCorrectUsage(context);
      return;
    }

    String characterName = request.arguments().get(0);

    long ep;
    try {
      ep = Long.parseLong(request.arguments().get(1));
    } catch (NumberFormatException e) {
      sendCorrectUsage(context);
      return;
    }

    long gp;
    try {
      gp = Long.parseLong(request.arguments().get(2));
    } catch (NumberFormatException e) {
      sendCorrectUsage(context);
      return;
    }

    if (gp < context.epgp().baseGP()) {
      sendError(context, "Expected <gp> >= BASE GP");
      return;
    }
    gp -= context.epgp().baseGP();

    try (Transaction tx = context.database().transaction()) {
      performEPGPUpdate(context, tx, EPGPEventType.IMPORT, ep, gp, null, null, null, ImmutableList.of(characterName), ImmutableList.of(), false);
    }
  }

  @Override
  public String help() {
    return "<character:string> <ep:int> <gp:int> - Imports EPGP from an external source.";
  }

  @Override
  public String command() {
    return "epgp.import";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.IMPORT_EPGP);
  }
}
