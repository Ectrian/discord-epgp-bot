package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.transport.User;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class TransportPermissionsCommandHandler extends CommandHandlerAbstract {

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    User user = context.user();

    if (!request.mentions().isEmpty()) {
      user = request.mentions().get(0);
    }

    context.replyf("Permissions for **%s**: [%s]",
        user.transportUserName(),
        Joiner.on(", ").join(user.permissions()));
  }

  @Override
  public String help() {
    return "[<user:@ref>] - Displays permissions available to a Discord account.";
  }

  @Override
  public String command() {
    return "transport.permissions";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
