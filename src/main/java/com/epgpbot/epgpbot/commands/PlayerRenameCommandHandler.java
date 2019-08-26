package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class PlayerRenameCommandHandler extends CommandHandlerAbstract {
  // TODO: Audit logging.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    // TODO
  }

  @Override
  public String help() {
    return "<old_name:string> <new_name:string> - Renames a player.";
  }

  @Override
  public String command() {
    return "player.rename";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_PERMISSIONS);
  }
}
