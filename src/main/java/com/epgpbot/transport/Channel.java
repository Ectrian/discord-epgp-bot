package com.epgpbot.transport;

import java.io.IOException;

import net.dv8tion.jda.core.entities.MessageChannel;

public interface Channel {
  public boolean isPrivate();
  public void reply(String message) throws IOException;
  public MessageChannel raw();
}
