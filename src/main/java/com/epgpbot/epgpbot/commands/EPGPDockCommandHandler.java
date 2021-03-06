package com.epgpbot.epgpbot.commands;

import java.util.List;
import java.util.Optional;

import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class EPGPDockCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    final long ep = request.arg("ep", 0).longValueAtLeast(0);
    final String note = request.arg("reason", 1).stringValue();
    final List<String> characterNames = request.argumentsFrom(2);

    try (Transaction tx = context.database().transaction()) {
      performEPGPUpdate(
          context,
          tx,
          EPGPEventType.PENALTY,
          -1 * ep,
          0,
          null,
          null,
          note,
          characterNames,
          ImmutableList.of(),
          false,
          Optional.empty());
    }
  }

  @Override
  public String help() {
    return "<ep:int> <reason:string> <...character:string> - Removes EP for poor performance.";
  }

  @Override
  public String command() {
    return "epgp.dock";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
