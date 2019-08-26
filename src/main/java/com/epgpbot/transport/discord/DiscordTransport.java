package com.epgpbot.transport.discord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.epgpbot.database.Database;
import com.epgpbot.transport.EventHandler;
import com.epgpbot.transport.Transport;
import com.epgpbot.transport.discord.ReactionManager.ReactionListener;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.StatusChangeEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class DiscordTransport extends ListenerAdapter implements Transport {
  @Override
  public void onMessageReactionAdd(MessageReactionAddEvent event) {
    reactionManager.onReactionAdd(event);
  }

  @Override
  public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
    reactionManager.onReactionRemove(event);
  }

  private JDA jda;
  private EventHandler handler;
  private String token;
  private BlockingQueue<Boolean> quitQ;
  private ReactionManager reactionManager;

  public DiscordTransport(String token) throws Exception {
    this.token = token;
    this.quitQ = new ArrayBlockingQueue<>(1);
    this.reactionManager = new ReactionManager();
  }

  @Override
  public void close() throws Exception {
    if (jda != null) {
      jda.removeEventListener(this);
      jda.shutdown();
    }
    reactionManager.close();
  }

  @Override
  public void run(EventHandler handler) throws Exception {
    System.out.format("[Transport] Connecting...\n");
    this.handler = handler;
    JDABuilder builder = new JDABuilder(token);
    builder.setAutoReconnect(true);
    builder.addEventListener(this);
    this.jda = builder.build();
    jda.awaitReady();
    quitQ.take();
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    try {
      User user = event.getAuthor();

      if (user.isBot()) {
        return;  // Ignore.
      }

      Message message = event.getMessage();

      if (message.isWebhookMessage()) {
        return;  // Ignore.
      }

      Database db = handler.getDatabase();
      String text = message.getContentDisplay();

      List<User> mentions = message.getMentionedUsers();
      List<com.epgpbot.transport.User> mentionedUsers = new ArrayList<>();
      for (User u : mentions) {
        mentionedUsers.add(new DiscordUser(db, handler.config(), u, null));
      }

      if (event.isFromType(ChannelType.TEXT)) {
        Guild guild = event.getGuild();
        TextChannel channel = event.getTextChannel();
        Member member = event.getMember();

        if (!channel.getGuild().getId().equals(handler.config().discord_server_id)) {
          return;  // Ignore messages not from the correct server.
        }
        if (!handler.config().discord_channels.contains(channel.getName())) {
          return; // Ignore messages not in allowed channels.
        }

        System.out.printf("[MessageEvent] (%s)[%s]<%s>: %s\n",
            guild.getName(),
            channel.getName(),
            member.getEffectiveName(),
            text);
        handler.handleIncomingMessage(
            this,
            new DiscordGuildChannel(channel),
            new DiscordUser(db, handler.config(), user, member),
            text,
            mentionedUsers
          );
      } else if (event.isFromType(ChannelType.PRIVATE)) {
        PrivateChannel channel = event.getPrivateChannel();

        // In public channels, we should only reply to commands to avoid spamming.
        // Otherwise, people just chatting in the channel will trigger the bot.
        if (!text.startsWith("!")) {
          channel
              .sendMessage("I don't recognize that command - reply with !help to see a list of available commands.")
              .queue();
          return;
        }

        System.out.printf("[MessageEvent] [DIRECT]<%s>: %s\n", user.getName(), text);
        handler.handleIncomingMessage(
            this,
            new DiscordPrivateMessageChannel(channel),
            new DiscordUser(db, handler.config(), user, null),
            text,
            mentionedUsers
          );
      }
    } catch (IOException e) {
      e.printStackTrace(System.err);
    } catch (Exception e) {
      // XXX: Should probably send an error reply.
      e.printStackTrace(System.err);
    }
  }

  @Override
  public JDA raw() {
    return jda;
  }

  @Override
  public void onReady(ReadyEvent event) {
    System.out.printf("[Transport] Connection established.\n");
  }

  @Override
  public void onDisconnect(DisconnectEvent event) {
    System.out.printf("[Transport] Connection lost.\n");
  }

  @Override
  public void onReconnect(ReconnectedEvent event) {
    System.out.printf("[Transport] Connection established.\n");
  }

  @Override
  public void onStatusChange(StatusChangeEvent event) {
    System.out.printf("[Transport] Status: %s\n", event.getNewStatus());
  }

  @Override
  public void quit() {
    try {
      quitQ.put(true);
    } catch (InterruptedException e) {
      System.exit(0);
    }
  }

  @Override
  public void registerReactionListener(String messageId, ReactionListener listener) {
    reactionManager.registerListener(messageId, listener);
  }
}
