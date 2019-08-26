package com.epgpbot.epgpbot.commands;

import java.io.IOException;
import java.util.List;

import com.epgpbot.armory.transport.ArmoryRequest;
import com.epgpbot.armory.transport.ArmoryResponse;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class GuildSyncCommandHandler extends CommandHandlerAbstract {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    context.reply("I have begun synchronizing the guild roster with the armory. I'll let you know when I'm finished.");

    ArmoryRequest armoryRequest = new ArmoryRequest()
        .setSyncGuild()
        .setForced(request.hasFlag("force"))
        .setCallback((ArmoryResponse rsp) -> {
          try {
            if (rsp.didSucceed) {
              context.replyf("Finished synchronizing %d guild characters.", rsp.totalCharacterCount);
            } else {
              context.replyf("I was unable to fully complete your request to synchronize the guild:\n%s",
                             Joiner.on("\n").join(rsp.errors));
            }
          } catch (IOException e) {
            // Ignore.
          }
        });

    if (!context.armory().offer(armoryRequest)) {
      context.reply("I am too busy to service your request at this time - try again later.");
      return;
    }
  }

  @Override
  public String help() {
    return "[--force] - Synchronizes all guild characters with the armory.";
  }

  @Override
  public String command() {
    return "guild.sync";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.ARMORY_SYNC);
  }
}
