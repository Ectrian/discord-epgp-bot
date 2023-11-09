package com.epgpbot.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.User;
import com.epgpbot.transport.discord.DiscordUser;
import com.google.common.base.Joiner;

public class Argument {
  public static class InvalidArgumentException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidArgumentException(String name, String value, String reason) {
      super((name == null) ?
          String.format("Invalid value `%s`: %s.", (value == null || value.length() == 0) ? "(nil)" : value, reason) :
          String.format("Invalid value `%s` for **%s**: %s.", (value == null || value.length() == 0) ? "(nil)" : value, name, reason));
    }
  }

  private final String name;
  private final String value;

  public Argument(String name, String value) {
    this.name = name;
    this.value = value;
  }

  private void require(boolean condition, String error) {
    if (!condition) {
      throw new InvalidArgumentException(name, value, error);
    }
  }

  public boolean isEmpty() {
    return value == null || value.length() == 0;
  }

  public Optional<String> stringOption() {
    if (isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  public String stringValue() {
    Optional<String> option = stringOption();
    require(option.isPresent(), "expected non-empty string");
    return option.get();
  }

  public Optional<Long> longOption() {
    if (isEmpty()) {
      return Optional.empty();
    }

    try {
      return Optional.of(Long.parseLong(value));
    } catch (NumberFormatException e) {
      require(false, "expected integer");
      return Optional.empty();
    }
  }

  public long longValue() {
    Optional<Long> option = longOption();
    require(option.isPresent(), "expected integer");
    return option.get();
  }

  public long longValueAtLeast(long n) {
    long value = longValue();
    require(value >= n, String.format("expected integer >= %d", n));
    return value;
  }

  public long longValueAtMost(long n) {
    long value = longValue();
    require(value <= n, String.format("expected integer >= %d", n));
    return value;
  }

  public long longValueBetween(long min, long max) {
    long value = longValue();
    require(value >= min && value <= max, String.format("expected integer between %d and %d", min, max));
    return value;
  }


  public Optional<Integer> intOption() {
    if (isEmpty()) {
      return Optional.empty();
    }

    try {
      return Optional.of(Integer.parseInt(value));
    } catch (NumberFormatException e) {
      require(false, "expected integer");
      return Optional.empty();
    }
  }

  public int intValue() {
    Optional<Integer> option = intOption();
    require(option.isPresent(), "expected integer");
    return option.get();
  }

  public Optional<LootInfo> lootOption(Transaction tx) throws Exception {
    if (isEmpty()) {
      return Optional.empty();
    }

    LootInfo loot = LootInfo.searchForSingleMatch(tx, value);
    require(loot != null, "unknown or ambiguous item name");
    return Optional.ofNullable(loot);
  }

  public LootInfo lootValue(Transaction tx) throws Exception {
    Optional<LootInfo> option = lootOption(tx);
    require(option.isPresent(), "unknown or ambiguous item name");
    return option.get();
  }

  public <T> Optional<T> enumOption(Class<T> type) {
    if (isEmpty()) {
      return Optional.empty();
    }

    T[] constants = type.getEnumConstants();

    if (value.matches("[0-9]+")) {
      int offset = intValue();
      if (offset >= 0 && offset < constants.length) {
        return Optional.of(constants[offset]);
      } else {
        require(false, String.format("expected enum %s {%s}",
          type.getSimpleName(),
          Joiner.on("|").join(type.getEnumConstants())));
        return Optional.empty();
      }
    }

    for (T constant : constants) {
      if (value.equalsIgnoreCase(constant.toString())) {
        return Optional.of(constant);
      }
    }

    require(false, String.format("expected enum %s {%s}",
        type.getSimpleName(),
        Joiner.on("|").join(type.getEnumConstants())));
    return Optional.empty();
  }

  public <T> T enumValue(Class<T> type) {
    Optional<T> option = enumOption(type);
    require(option.isPresent(), String.format("expected enum %s {%s}",
        type.getSimpleName(),
        Joiner.on("|").join(type.getEnumConstants())));
    return option.get();
  }

  public Optional<PlayerId> characterPlayerIdOption(Transaction tx) throws Exception {
    if (isEmpty()) {
      return Optional.empty();
    }

    try (Statement q = tx.prepare(
          "SELECT p.name, c.player_id, c.id AS character_id "
        + "FROM characters AS c "
        + "JOIN players AS p ON p.id = c.player_id "
        + "WHERE lower(c.name) = :name AND c.deleted = 0 AND c.player_id IS NOT NULL;")) {
      q.bind("name", value.toLowerCase());
      try (Cursor r = q.executeFetch()) {
        if (!r.next()) {
          require(false, "unknown or unlinked character");
          return Optional.empty();
        }
        return Optional.of(PlayerId.withTransportInfo(tx,
            r.get("player_id", Long.class),
            r.get("character_id", Long.class),
            r.get("name", String.class)));
      }
    }
  }

  public PlayerId characterPlayerIdValue(Transaction tx) throws Exception {
    Optional<PlayerId> option = characterPlayerIdOption(tx);
    require(option.isPresent(), "unknown or unlinked character");
    return option.get();
  }

  public Optional<PlayerId> playerIdOption(Transaction tx) throws Exception {
    if (isEmpty()) {
      return Optional.empty();
    }

    try (Statement q = tx.prepare("SELECT id, name FROM players WHERE lower(name) = :name;")) {
      q.bind("name", value.toLowerCase());
      try (Cursor r = q.executeFetch()) {
        if (!r.next()) {
          require(false, "unknown player");
          return Optional.empty();
        }
        return Optional.of(PlayerId.withTransportInfo(tx,
            r.get("id", Long.class),
            -1,
            r.get("name", String.class)));
      }
    }
  }

  public PlayerId playerIdValue(Transaction tx) throws Exception {
    Optional<PlayerId> option = playerIdOption(tx);
    require(option.isPresent(), "unknown player");
    return option.get();
  }

  public Optional<Long> timeOption() {
    if (isEmpty()) {
      return Optional.empty();
    }

    if (value.matches("[0-9]+")) {
      return longOption();
    }

    try {
      SimpleDateFormat fmt = new SimpleDateFormat("MM-dd-yyyy hh:mm a zzz");
      Date time = fmt.parse(value);
      return Optional.of(time.toInstant().getEpochSecond());
    } catch (Exception e) {
      require(false, "unable to parse time (expected format `7-21-1993 12:15 PM PST`)");
      return Optional.empty();
    }
  }

  public Long timeValue() {
    Optional<Long> option = timeOption();
    require(option.isPresent(), "unable to parse time (expected format `7-21-1993 12:15 PM PST`)");
    return option.get();
  }

  private User getTransportUser(CommandContext context, String name, int discriminator) throws Exception {
    for (net.dv8tion.jda.api.entities.User user : context.transport().raw().getUsersByName(name, true)) {
      if (user.getDiscriminator().equals(String.format("%d", discriminator))) {
        return new DiscordUser(context.database(), context.config(), user, null);
      }
    }
    require(false, "invalid user reference (not found)");
    return null;
  }

  private User getTransportUser(CommandContext context, String name) throws Exception {
    if (name.matches("^<@!?[0-9]+>$")) {
      String id;
      if (name.startsWith("<@!")) {
        id = name.substring(3, name.length() - 1);  // older versions of discord
      }
      else {
        id = name.substring(2, name.length() - 1);  // newer versions of discord
      }
      // System.out.format("finding name='%s' has id id='%s'", name, id);
      net.dv8tion.jda.api.entities.User user = context.transport().raw().getUserById(id);
      require(user != null, "invalid user reference (not found)");
      return new DiscordUser(context.database(), context.config(), user, null);
    }

    if (name.startsWith("@")) {
      name = name.substring(1, name.length());
    }

    int separator = name.lastIndexOf("#");

    if (separator < 0 || separator == name.length() - 1) {
      require(false, "invalid user reference (expected name#id)");
      return null;
    }

    String username = name.substring(0, separator);
    String discriminator = name.substring(separator + 1, name.length());
    int discriminatorAsInt;

    try {
      discriminatorAsInt = Integer.parseInt(discriminator);
    } catch (NumberFormatException e) {
      require(false, "invalid user reference (expected name#id)");
      return null;
    }

    return getTransportUser(context, username, discriminatorAsInt);
  }

  public Optional<User> userOption(CommandContext context) throws Exception {
    if (isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(getTransportUser(context, value));
  }

  public User userValue(CommandContext context) throws Exception {
    Optional<User> option = userOption(context);
    require(option.isPresent(), "invalid user reference");
    return option.get();
  }

  public Optional<Long> characterIdOption(Transaction tx) throws Exception {
    if (isEmpty()) {
      return Optional.empty();
    }

    try (Statement q = tx.prepare("SELECT id FROM characters WHERE lower(name) = :name;")) {
      q.bind("name", value.toLowerCase());
      try (Cursor r = q.executeFetch()) {
        require(r.next(), "unknown character name");
        return Optional.of(r.get("id", Long.class));
      }
    }
  }

  public Long characterIdValue(Transaction tx) throws Exception {
    Optional<Long> option = characterIdOption(tx);
    require(option.isPresent(), "unknown character name");
    return option.get();
  }
}
