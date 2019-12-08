package com.epgpbot.epgpbot.commands;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.epgpbot.config.Config;
import com.epgpbot.database.Cursor;
import com.epgpbot.database.Database;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGP;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.transport.discord.DiscordUser;
import com.epgpbot.transport.discord.ReactionManager;
import com.epgpbot.transport.discord.ReactionManager.ReactionListener;
import com.epgpbot.util.TextTable;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;

class InteractiveCompareListener implements ReactionListener {
  private static final ImmutableMap<String, Integer> SPEC_SORT =
      new ImmutableMap.Builder<String, Integer>()
          .put("MS", 3)
          .put("OS", 2)
          .put("FUN", 1)
          .build();
  public static final int TIMEOUT_SEC = 30 * 60;
  // public static final String REACTION_CHECKMARK = "\u2611";
  public static final String REACTION_MS = "🇲";
  public static final String REACTION_OS = "🇴";
  public static final String REACTION_YOLO = "🇫";

  private final SecureRandom prng;
  private final Set<String> pinnedCharacters;
  private final Config config;
  private final EPGP epgp;
  private final Database db;
  private final String messageId;
  private final boolean details;
  private final String title;
  private Map<User, String> reactions;
  private Map<Long, Long> rolls;
  private long expireTime;

  public InteractiveCompareListener(
      Config config,
      Database db,
      EPGP epgp,
      String messageId,
      List<String> pinnedCharacters,
      String title,
      boolean details,
      MessageChannel channel) {
    this.config = config;
    this.db = db;
    this.epgp = epgp;
    this.messageId = messageId;
    this.pinnedCharacters = ImmutableSet.copyOf(pinnedCharacters);
    this.title = title;
    this.details = details;
    this.reactions = new HashMap<>();
    this.rolls = new HashMap<>();
    this.expireTime = Instant.now().getEpochSecond() + TIMEOUT_SEC;
    this.prng = new SecureRandom();
    updateMessage(channel);
  }

  private long getRoll(Long playerId) {
    if (rolls.containsKey(playerId)) {
      return rolls.get(playerId);
    }

    double value = prng.nextDouble();
    long roll = Math.round(1 + (value * (100 - 1)));
    rolls.put(playerId, roll);
    return roll;
  }

  private void updateMessage(MessageChannel channel) {
    channel.getMessageById(messageId).queue((Message m) -> {
        try {
          // XXX: Perhaps we should rate-limit the updates?
          m.editMessage(render()).queue();
        } catch (Exception e) {
          e.printStackTrace(System.err);
        }
    });
  }

  private String render() throws Exception {
    String out = "**EPGP Comparison:**\n";
    if (!title.isEmpty()) {
      out = String.format("**%s:**\n", title);
    }

    List<String> fields = ImmutableList.of("player", "character", "spec", "ep", "gp", "priority", "roll");
    if (details) {
      fields = ImmutableList.of("player", "character", "spec", "ep", "gp", "priority", "roll", "ep_earned", "gp_earned", "items_won", "avg_item_cost");
    }
    List<String> missingCharacterNames = new ArrayList<>();
    List<String> missingPlayerRefs = new ArrayList<>();
    List<Map<String, Object>> data = generateTable(missingCharacterNames, missingPlayerRefs);
    data.sort(new Comparator<Map<String, Object>>() {
      @Override
      public int compare(Map<String, Object> a, Map<String, Object> b) {
        int aSpec = SPEC_SORT.getOrDefault(a.get("spec"), 0);
        int bSpec = SPEC_SORT.getOrDefault(b.get("spec"), 0);
        if (aSpec < bSpec) {
          return 1;
        }
        if (aSpec > bSpec) {
          return -1;
        }

        double aPriority = (Double)a.get("raw_priority");
        double bPriority = (Double)b.get("raw_priority");
        if (aPriority < bPriority) {
          return 1;
        }
        if (aPriority > bPriority) {
          return -1;
        }

        double aRoll = (Long)a.get("roll");
        double bRoll = (Long)b.get("roll");
        if (aRoll < bRoll) {
          return 1;
        }
        if (aRoll > bRoll) {
          return -1;
        }

        return 0;
      }
    });

    out += "```\n" + TextTable.format(
      fields,
      data,
      ImmutableSet.of("ep", "gp", "ep_earned", "gp_earned", "items_won", "priority", "avg_item_cost", "roll")
    ) + "```\n";

    if (!missingCharacterNames.isEmpty()) {
      out += "Not Found: ";
      out += Joiner.on(", ").join(missingCharacterNames);
      out += "\n";
    }
    if (!missingPlayerRefs.isEmpty()) {
      out += "Not Linked: ";
      out += Joiner.on(", ").join(missingPlayerRefs);
      out += "\n";
    }
    return out;
  }

  private List<Map<String, Object>> generateTable(
      List<String> missingCharacterNames,
      List<String> missingPlayerRefs) throws Exception {
    List<Map<String, Object>> data = new ArrayList<>();
    Set<Long> playerIds = new HashSet<>();


    try (Transaction tx = db.transaction()) {
      for (String characterName : pinnedCharacters) {
        try (Statement q = tx.prepare(
              "SELECT p.id AS player_id, c.name AS character_name, p.name, p.ep_net, p.gp_net, p.ep_earned, p.gp_earned, (p.ep_net / (:base_gp + p.gp_net)) AS priority, "
            + "((SELECT COUNT(*) FROM epgp_log WHERE target_player_id = p.id AND type = :loot_type AND gp_delta > 0) - (SELECT COUNT(*) FROM epgp_log WHERE target_player_id = p.id AND type = :loot_type AND gp_delta < 0)) AS items_won "
            + "FROM players AS p "
            + "JOIN characters AS c ON c.player_id = p.id "
            + "WHERE lower(c.name) = :character_name "
            + "ORDER BY priority DESC, name ASC;")) {
          q.bind("base_gp", (double)epgp.baseGP());
          q.bind("character_name", characterName.toLowerCase());
          q.bind("loot_type", EPGPEventType.LOOT.ordinal());
          try (Cursor r = q.executeFetch()) {
            if (r.next()) {
              Map<String, Object> row = new HashMap<>();
              row.put("player", r.get("name", String.class));
              row.put("character", r.get("character_name", String.class));
              row.put("ep", r.get("ep_net", Long.class));
              row.put("gp", r.get("gp_net", Long.class) + epgp.baseGP());
              row.put("ep_earned", r.get("ep_earned", Long.class));
              row.put("gp_earned", r.get("gp_earned", Long.class));
              row.put("items_won", r.get("items_won", Long.class));
              row.put("priority", String.format("%.3f", r.get("priority", Double.class)));
              row.put("raw_priority", r.get("priority", Double.class));
              row.put("avg_item_cost", Math.round(r.get("gp_earned", Long.class) / (double)r.get("items_won", Long.class)));
              row.put("spec", "MS");
              row.put("roll", getRoll(r.get("player_id", Long.class)));
              playerIds.add(r.get("player_id", Long.class));
              data.add(row);
            } else {
              missingCharacterNames.add(characterName);
            }
          }
        }
      }

      for (User u : reactions.keySet()) {
        DiscordUser user = new DiscordUser(db, config, u, null);
        if (!user.hasPlayer()) {
          missingPlayerRefs.add(String.format("<@%s>", u.getId()));
          continue;
        }
        if (playerIds.contains(user.playerId())) {
          continue;
        }
        playerIds.add(user.playerId());

        try (Statement q = tx.prepare(
            "SELECT p.id AS player_id, p.name, p.ep_net, p.gp_net, p.ep_earned, p.gp_earned, (p.ep_net / (:base_gp + p.gp_net)) AS priority, "
          + "((SELECT COUNT(*) FROM epgp_log WHERE target_player_id = p.id AND type = :loot_type AND gp_delta > 0) - (SELECT COUNT(*) FROM epgp_log WHERE target_player_id = p.id AND type = :loot_type AND gp_delta < 0)) AS items_won "
          + "FROM players AS p "
          + "WHERE p.id = :player_id "
          + "ORDER BY priority DESC, name ASC;")) {
          q.bind("base_gp", (double)epgp.baseGP());
          q.bind("player_id", user.playerId());
          q.bind("loot_type", EPGPEventType.LOOT.ordinal());
          try (Cursor r = q.executeFetch()) {
            if (r.next()) {
              Map<String, Object> row = new HashMap<>();
              row.put("player", r.get("name", String.class));
              row.put("character", Optional.empty());
              row.put("ep", r.get("ep_net", Long.class));
              row.put("gp", r.get("gp_net", Long.class) + epgp.baseGP());
              row.put("ep_earned", r.get("ep_earned", Long.class));
              row.put("gp_earned", r.get("gp_earned", Long.class));
              row.put("items_won", r.get("items_won", Long.class));
              row.put("priority", String.format("%.3f", r.get("priority", Double.class)));
              row.put("raw_priority", r.get("priority", Double.class));
              row.put("avg_item_cost", Math.round(r.get("gp_earned", Long.class) / (double)r.get("items_won", Long.class)));

              String reaction = reactions.get(u);
              if (REACTION_MS.equals(reaction)) {
                row.put("spec", "MS");
              } else if (REACTION_OS.equals(reaction)) {
                row.put("spec", "OS");
              } else {
                row.put("spec", "FUN");
              }

              row.put("roll", getRoll(user.playerId()));

              playerIds.add(r.get("player_id", Long.class));
              data.add(row);
            }
          }
        }
      }
    }

    return data;
  }

  @Override
  public boolean isExpired() {
    return Instant.now().getEpochSecond() > expireTime;
  }

  @Override
  public void onReactionAdded(ReactionManager manager, MessageReactionAddEvent event) {
    if (!event.getMessageId().equals(messageId)) {
      return;
    }
    if (event.getUser().isBot()) {
      return;
    }
    reactions.put(event.getUser(), event.getReactionEmote().getName());
    expireTime = Instant.now().getEpochSecond() + TIMEOUT_SEC;
    updateMessage(event.getChannel());
  }

  @Override
  public void onReactionRemoved(ReactionManager manager, MessageReactionRemoveEvent event) {
    if (!event.getMessageId().equals(messageId)) {
      return;
    }
    reactions.remove(event.getUser());
    expireTime = Instant.now().getEpochSecond() + TIMEOUT_SEC;
    updateMessage(event.getChannel());
  }

  public static void send(CommandContext context, List<String> characterNames, boolean details, String title) {
    context.source().raw()
      .sendMessage("(initializing...)")
      .queue((Message m) -> {
        context.transport().registerReactionListener(
          m.getId(),
          new InteractiveCompareListener(
              context.config(),
              context.database(),
              context.epgp(),
              m.getId(),
              characterNames,
              title,
              details,
              m.getChannel())
        );
        // m.addReaction(REACTION_CHECKMARK).queue();
        m.addReaction(REACTION_MS).queue();
        m.addReaction(REACTION_OS).queue();
        m.addReaction(REACTION_YOLO).queue();
      });
  }
}

public class EPGPCompareInteractiveCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    InteractiveCompareListener.send(context, request.arguments(), request.hasFlag("details"), Joiner.on(" ").join(request.flag("title")));
  }

  @Override
  public String help() {
    return "<...character:string> [--details] [--title <:string>] - Compares EPGP across multiple characters. Users may react/un-react to add themselves to the comparison.";
  }

  @Override
  public String command() {
    return "epgp.compare.interactive";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
