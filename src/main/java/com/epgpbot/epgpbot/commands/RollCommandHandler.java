package com.epgpbot.epgpbot.commands;

import java.security.SecureRandom;
import java.util.List;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class RollCommandHandler extends CommandHandlerAbstract {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    SecureRandom prng = new SecureRandom();
    long min = getLongArg(request, 0).orElse(1L);
    long max = getLongArg(request, 1).orElse(100L);

    if (min < 0 || max < 0 || min >= max) {
      sendCorrectUsage(context);
      return;
    }

    double value = prng.nextDouble();
    long roll = Math.round(min + (value * (max - min)));

    context.replyf("%s rolls %d (%d - %d).",
        context.user().transportUserName(),
        roll,
        min,
        max);
  }

  @Override
  public String help() {
    return "[<min:int>] [<max:int>] - Rolls a random number.";
  }

  @Override
  public String command() {
    return "roll";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
