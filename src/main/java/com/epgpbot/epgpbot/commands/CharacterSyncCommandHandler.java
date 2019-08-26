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

public class CharacterSyncCommandHandler extends CommandHandlerAbstract {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().isEmpty()) {
      sendCorrectUsage(context);
      return;
    }

    ArmoryRequest armoryRequest = new ArmoryRequest()
        .setForced(request.hasFlag("force"));

    if (request.arguments().get(0).equals("<all>")) {
      context.reply("I have begun synchronizing all characters with the armory. I'll let you know when I'm finished.");
      armoryRequest.setSyncAllCharacters();
      armoryRequest.setCallback((ArmoryResponse rsp) -> {
        try {
          if (rsp.didSucceed) {
            context.replyf("Your request to synchronize all characters has completed - %d characters updated.",
                           rsp.totalCharacterCount);
          } else {
            context.replyf("I was unable to fully complete your request to synchronize all characters:\n%s",
                           Joiner.on("\n").join(rsp.errors));
          }
        } catch (IOException e) {
          // Ignore.
        }
      });
    } else {
      context.replyf("I have begun synchronizing characters(s) '%s' with the armory. I'll let you know when I'm finished.",
                     Joiner.on(", ").join(request.arguments()));
      armoryRequest.addCharacters(request.arguments());
      armoryRequest.setCallback((ArmoryResponse rsp) -> {
        try {
          if (rsp.didSucceed) {
            context.replyf("Your request to synchronize characters(s) '%s' has completed - %d characters updated.",
                           Joiner.on(", ").join(request.arguments()),
                           rsp.totalCharacterCount);
          } else {
            context.replyf("I was unable to fully complete your request to synchronize characters(s) '%s':\n%s",
                           Joiner.on(", ").join(request.arguments()),
                           Joiner.on("\n").join(rsp.errors));
          }
        } catch (IOException e) {
          // Ignore.
        }
      });
    }

    if (!context.armory().offer(armoryRequest)) {
      context.reply("I am too busy to service your request at this time - try again later.");
      return;
    }
  }

  @Override
  public String help() {
    return "<...character:string|'<all>'> [--force] - Synchronizes characters with the armory.";
  }

  @Override
  public String command() {
    return "character.sync";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.ARMORY_SYNC);
  }
}
