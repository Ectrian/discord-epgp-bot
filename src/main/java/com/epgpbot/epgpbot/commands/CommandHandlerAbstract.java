package com.epgpbot.epgpbot.commands;

import java.io.IOException;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.base.Optional;

public abstract class CommandHandlerAbstract implements CommandHandler {
  public static class PlayerId {
    public final long id;
    public final String name;
    public final String transportUserId;

    public PlayerId(long id, String name, String transportUserId) {
      this.id = id;
      this.name = name;
      this.transportUserId = transportUserId;
    }

    public static PlayerId withTransportInfo(Transaction tx, long id, String name) throws Exception {
      try (Statement q = tx.prepare("SELECT id FROM transport_users WHERE player_id = :id;")) {
        q.bind("id", id);
        try (Cursor r = q.executeFetch()) {
          if (!r.next()) {
            return new PlayerId(id, name, null);
          }
          return new PlayerId(id, name, r.get("id", String.class));
        }
      }
    }
  }

  public Optional<Long> getLongArg(Request request, int offset) {
    if (offset >= request.arguments().size()) {
      return Optional.absent();
    }
    try {
      return Optional.of(Long.parseLong(request.arguments().get(offset)));
    } catch (NumberFormatException e) {
      return Optional.absent();
    }
  }

  // Tries to infer the player's identity based on any DB links or their discord username.
  // NOTE: NOT RELIABLE - PLAYERS CAN PRETEND TO BE OTHERS! DO NOT USE FOR PERMISSIONS!
  protected PlayerId getInferredPlayer(Transaction tx, CommandContext context) throws Exception {
    if (context.user().hasPlayer()) {
      return new PlayerId(context.user().playerId(), context.user().playerName(), context.user().transportUserId());
    }

    try (Statement q = tx.prepare("SELECT id, name FROM players WHERE lower(name) = :name;")) {
      q.bind("name", context.user().transportUserName());
      try (Cursor r = q.executeFetch()) {
        if (r.next()) {
          return PlayerId.withTransportInfo(tx, r.get("id", Long.class), r.get("name", String.class));
        }
      }
    }

    return null;
  }

  // Returns s with first character uppercased.
  protected String ucfirst(String s) {
    if (s.length() == 0) {
      return s;
    }
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  protected void sendCorrectUsage(CommandContext context) throws IOException {
    context.reply(String.format("*Incorrect usage*: **%s** %s",
        this.command(),
        this.help()));
  }

  protected void sendError(CommandContext context, String format, Object ...args) throws IOException {
    context.reply(String.format("*Failure*: " + format, args));
  }

  protected PlayerId getPlayerId(Transaction tx, String playerName) throws Exception {
    try (Statement q = tx.prepare("SELECT id, name FROM players WHERE lower(name) = :name;")) {
      q.bind("name", playerName.toLowerCase());
      try (Cursor r = q.executeFetch()) {
        if (!r.next()) {
          return null;
        }
        return PlayerId.withTransportInfo(tx, r.get("id", Long.class), r.get("name", String.class));
      }
    }
  }

  protected PlayerId getPlayerId(Transaction tx, long playerId) throws Exception {
    try (Statement q = tx.prepare("SELECT id, name FROM players WHERE id = :id;")) {
      q.bind("id", playerId);
      try (Cursor r = q.executeFetch()) {
        if (!r.next()) {
          return null;
        }
        return PlayerId.withTransportInfo(tx, r.get("id", Long.class), r.get("name", String.class));
      }
    }
  }

  protected String getCharacterName(Transaction tx, long id) throws Exception {
    try (Statement q = tx.prepare("SELECT name FROM characters WHERE id = :id;")) {
      q.bind("id", id);
      try (Cursor r = q.executeFetch()) {
        if (!r.next()) {
          return null;
        }
        return r.get("name", String.class);
      }
    }
  }

  protected PlayerId getPlayerIdForCharacter(Transaction tx, String characterName) throws Exception {
    try (Statement q = tx.prepare("SELECT p.name, c.player_id FROM characters AS c JOIN players AS p ON p.id = c.player_id WHERE lower(c.name) = :name AND c.deleted = 0 AND c.player_id IS NOT NULL;")) {
      q.bind("name", characterName.toLowerCase());
      try (Cursor r = q.executeFetch()) {
        if (!r.next()) {
          return null;
        }
        return PlayerId.withTransportInfo(tx, r.get("player_id", Long.class), r.get("name", String.class));
      }
    }
  }

  @Override
  public boolean visible() {
    return true;
  }
}