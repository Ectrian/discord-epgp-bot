package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class GuildOfficersCommandHandler extends GuildAbstractCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    showOnlinePlayers(context, request, true);
  }

  @Override
  public String help() {
    return "- Lists currently online officers.";
  }

  @Override
  public String command() {
    return "guild.officers";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
