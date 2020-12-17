package com.epgpbot.transport.discord;

import java.io.IOException;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;

public class DiscordPrivateMessageChannel extends DiscordChannel {
  private final PrivateChannel channel;

  public DiscordPrivateMessageChannel(PrivateChannel channel) {
    this.channel = channel;
  }

  @Override
  public void reply(String message) throws IOException {
    for (String part : chunk(message)) {
      channel.sendMessage(part).queue();
    }
  }

  @Override
  public boolean isPrivate() {
    return true;
  }

  @Override
  public MessageChannel raw() {
    return channel;
  }
}
