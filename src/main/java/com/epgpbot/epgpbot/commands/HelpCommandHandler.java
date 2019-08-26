package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class HelpCommandHandler extends CommandHandlerAbstract {
  private final List<CommandHandler> handlers;

  public HelpCommandHandler(List<CommandHandler> handlers) {
    this.handlers = handlers;
  }

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    StringBuilder message = new StringBuilder();
    boolean showPermissions = request.hasFlag("show-permissions");

    if (request.arguments().isEmpty()) {
      message.append("Hi! I am a bot that manages EPGP, statistics, and more.\n");
      message.append("To use me, simply reply with a command or send me a direct message.\n\n");
      message.append("**Available Commands (to __" + context.user().transportUserName() + "__):**\n");
    }

    for (CommandHandler h : handlers) {
      if (!h.visible()) {
        continue;
      }
      if (!context.user().hasPermissions(h.permissions())) {
        continue;
      }
      if (!request.arguments().isEmpty()) {
        boolean isMatch = false;

        for (String arg : request.arguments()) {
          if (h.command().startsWith(arg)) {
            isMatch = true;
            break;
          }
        }

        if (!isMatch) {
          continue;
        }
      }

      message.append("**!");
      message.append(h.command());
      message.append("** ");
      message.append(h.help());
      if (showPermissions && !h.permissions().isEmpty()) {
        message.append(" - requires ");
        message.append(Joiner.on(",").join(h.permissions()));
      }
      message.append("\n");
    }

    if (message.toString().isEmpty()) {
      context.reply("I'm not aware of any commands with that name.");
    } else {
      context.reply(message.toString());
    }
  }

  @Override
  public String help() {
    return "[<command:string>] [--show-permissions] - Displays information about available commands.";
  }

  @Override
  public String command() {
    return "help";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of();
  }
}
