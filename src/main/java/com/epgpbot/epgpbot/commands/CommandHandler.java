package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;

public interface CommandHandler {
  public void handle(CommandContext context, Request request) throws Exception;
  public String help();
  public String command();
  public List<PermissionType> permissions();
  public boolean visible();
}
