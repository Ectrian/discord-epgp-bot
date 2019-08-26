package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class QuitCommandHandler extends CommandHandlerAbstract {

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    context.transport().quit();
  }

  @Override
  public String help() {
    return " - Terminates the bot.";
  }

  @Override
  public String command() {
    return "quit";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_PERMISSIONS);
  }

}
