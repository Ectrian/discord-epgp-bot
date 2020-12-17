package com.epgpbot.transport;

import com.epgpbot.transport.discord.ReactionManager.ReactionListener;

import net.dv8tion.jda.api.JDA;

public interface Transport extends AutoCloseable {
  public void registerReactionListener(String messageId, ReactionListener listener);
  public void run(EventHandler handler) throws Exception;
  public void quit();
  public JDA raw();  // XXX: Probably don't want to expose this.
}
