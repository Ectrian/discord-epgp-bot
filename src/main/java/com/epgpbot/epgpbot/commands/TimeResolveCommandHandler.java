package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class TimeResolveCommandHandler extends CommandHandlerAbstract {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    long time = request.arg("time", 0).timeValue();
    context.replyf("%d %s", time, formatDate(time));
  }

  @Override
  public String help() {
    return "<time:datetime> - Prints a parsed time (for debugging).";
  }

  @Override
  public String command() {
    return "time.parse";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
