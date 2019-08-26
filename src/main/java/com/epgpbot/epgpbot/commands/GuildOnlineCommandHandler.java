package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class GuildOnlineCommandHandler extends GuildAbstractCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    showOnlinePlayers(context, request, false);
  }

  @Override
  public String help() {
    return "- Lists currently online guild members.";
  }

  @Override
  public String command() {
    return "guild.members";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
