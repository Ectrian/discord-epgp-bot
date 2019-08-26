package com.epgpbot.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.epgpbot.config.Config;
import com.epgpbot.database.Database;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.discord.DiscordChannel;
import com.epgpbot.transport.discord.DiscordUser;
import com.epgpbot.transport.discord.ReactionManager;
import com.epgpbot.transport.discord.ReactionManager.ReactionListener;
import com.google.common.collect.ImmutableList;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.requests.RequestFuture;

public class MessagePaginator implements ReactionListener {
  public interface PageSource {
    public int pageCount();

    public String renderPage(int page);

    public MessageEmbed renderPageEmbed(int page);
  }

  public static final int TIMEOUT_SEC = 30 * 60;
  public static final String REACTION_FIRST = "\u23EA";
  public static final String REACTION_PREV = "\u25C0";
  public static final String REACTION_NEXT = "\u25B6";
  public static final String REACTION_LAST = "\u23E9";

  private final List<String> messageIds;
  private final String listenMessageId;
  private final String userId;
  private final PageSource source;
  private final Database db;
  private final Config config;
  private long expireTime;
  private int currentPage;

  public MessagePaginator(List<String> messageIds, String userId, PageSource source, Database db,
      Config config) {
    this.messageIds = messageIds;
    this.listenMessageId = messageIds.get(messageIds.size() - 1);
    this.userId = userId;
    this.source = source;
    this.expireTime = Instant.now().getEpochSecond() + TIMEOUT_SEC;
    this.currentPage = 0;
    this.db = db;
    this.config = config;
  }

  @Override
  public boolean isExpired() {
    return Instant.now().getEpochSecond() > expireTime;
  }

  private void onReactionUpdated(GenericMessageReactionEvent event) {
    DiscordUser user;

    if (!event.getMessageId().equals(listenMessageId)) {
      return;
    }

    try {
      user = new DiscordUser(db, config, event.getUser(), event.getMember());
    } catch (Exception e) {
      return;
    }

    boolean canPage = event.getUser().getId().equals(userId)
        || (user != null && user.hasPermission(PermissionType.MODIFY_EPGP));

    if (!canPage) {
      return;
    }

    expireTime = Instant.now().getEpochSecond() + TIMEOUT_SEC;

    switch (event.getReactionEmote().getName()) {
      case REACTION_FIRST:
        currentPage = 0;
        break;
      case REACTION_LAST:
        currentPage = source.pageCount() - 1;
        break;
      case REACTION_NEXT:
        if (currentPage + 1 < source.pageCount()) {
          currentPage++;
        }
        break;
      case REACTION_PREV:
        if (currentPage > 0) {
          currentPage--;
        }
        break;
    }

    MessageEmbed embed = source.renderPageEmbed(currentPage);
    if (embed != null) {
      event.getChannel().getMessageById(event.getMessageId()).queue((Message m) -> {
        m.editMessage(embed).queue();
      });
    }

    String messageText = source.renderPage(currentPage);
    if (messageText != null) {
      Iterator<String> messageChunks =
          TextChunker.chunk(messageText, DiscordChannel.MESSAGE_MAX_LEN).iterator();

      for (String messageId : messageIds) {
        if (messageChunks.hasNext()) {
          String chunk = messageChunks.next();
          event.getChannel().getMessageById(messageId).queue((Message m) -> {
            if (!m.getContentRaw().equals(chunk)) {
              m.editMessage(chunk).queue();
            }
          });
        } else {
          event.getChannel().getMessageById(messageId).queue((Message m) -> {
            if (!m.getContentRaw().equals(".")) {
              m.editMessage(".").queue();
            }
          });
        }
      }
    }
  }

  @Override
  public void onReactionAdded(ReactionManager manager, MessageReactionAddEvent event) {
    onReactionUpdated(event);
  }

  @Override
  public void onReactionRemoved(ReactionManager manager, MessageReactionRemoveEvent event) {
    onReactionUpdated(event);
  }

  public static void sendPaginatedMessage(CommandContext context, PageSource source) {
    String message = source.renderPage(0);
    MessageEmbed embed = source.renderPageEmbed(0);
    if (message != null) {
      List<RequestFuture<Message>> messages = new ArrayList<>();

      int lastChunkSize = 0;
      for (String text : TextChunker.chunk(message, DiscordChannel.MESSAGE_MAX_LEN)) {
        messages.add(context.source().raw().sendMessage(text).submit());
        lastChunkSize = text.length();
      }
      // Try to allocate an additional message in case it is needed later.
      if (DiscordChannel.MESSAGE_MAX_LEN - lastChunkSize <= 400) {
        messages.add(context.source().raw().sendMessage(".").submit());
      }


      CompletableFuture.allOf(messages.toArray(new CompletableFuture[messages.size()]))
          .thenRun(() -> {
            try {
              Message lastMessage = null;
              List<String> messageIds = new ArrayList<>();

              for (RequestFuture<Message> f : messages) {
                lastMessage = f.get();
                messageIds.add(lastMessage.getId());
              }

              if (source.pageCount() > 1) {
                addControls(lastMessage);
              }
              context.transport().registerReactionListener(lastMessage.getId(),
                  new MessagePaginator(messageIds, context.user().transportUserId(), source,
                      context.database(), context.config()));
            } catch (Exception e) {
              e.printStackTrace(System.err);
            }
          });

    } else if (embed != null) {
      context.source().raw().sendMessage(embed).queue((Message m) -> {
        if (source.pageCount() > 1) {
          addControls(m);
        }
        context.transport().registerReactionListener(m.getId(),
            new MessagePaginator(ImmutableList.of(m.getId()), context.user().transportUserId(),
                source, context.database(), context.config()));
      });
    }
  }

  public static void addControls(Message m) {
    m.addReaction(REACTION_FIRST).queue();
    m.addReaction(REACTION_PREV).queue();
    m.addReaction(REACTION_NEXT).queue();
    m.addReaction(REACTION_LAST).queue();
  }
}
