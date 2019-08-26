package com.epgpbot.transport.discord;

import com.epgpbot.transport.Channel;
import com.epgpbot.util.TextChunker;
import com.google.common.collect.ImmutableList;

import net.dv8tion.jda.core.entities.MessageChannel;

public abstract class DiscordChannel implements Channel {
  // Hard message limit enforced by Discord.
  public static final int MESSAGE_MAX_LEN = 2000;

  public Iterable<String> chunk(String message) {
    if (message.length() <= MESSAGE_MAX_LEN) {
      return ImmutableList.of(message);
    }

    /*
     * Things get tricky here - we need to chunk the message across
     * multiple messages. But we can't split the message at arbitrary
     * positions. We need to try to split on new lines and - if we
     * split within a markdown code block - we need to end the last
     * chunk with the block terminator and repeat the block opener
     * at the start of the next message.
     */
    return TextChunker.chunk(message, MESSAGE_MAX_LEN);
  }

  @Override
  public abstract MessageChannel raw();
}
