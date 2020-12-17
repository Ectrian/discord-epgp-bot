package com.epgpbot.transport.discord;

import java.io.IOException;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;

public class DiscordGuildChannel extends DiscordChannel {
  private final TextChannel channel;

  public DiscordGuildChannel(TextChannel channel) {
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
    return false;
  }

  @Override
  public MessageChannel raw() {
    return channel;
  }
}
