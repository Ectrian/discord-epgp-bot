package com.epgpbot.epgpbot.schema;

import java.awt.Color;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.game.ItemRarity;
import com.google.common.collect.ImmutableList;

public class LootInfo {
  private static final String GET_QUERY = "SELECT l.id, l.name, lgi.game_id, lgi.game_rarity "
      + "FROM loot AS l " + "LEFT JOIN loot_game_info AS lgi ON lgi.loot_id = l.id " + "WHERE %s "
      + "ORDER BY l.name ASC;";
  private static final String GET_ALIAS_QUERY = "SELECT l.id, l.name, lgi.game_id, lgi.game_rarity "
      + "FROM loot AS l " + "LEFT JOIN loot_game_info AS lgi ON lgi.loot_id = l.id "
      + "LEFT JOIN loot_alias AS la ON la.loot_id = l.id " + "WHERE %s " + "ORDER BY l.name ASC;";
  public long lootId;
  public String name;

  // Note: Loot may have multiple game items attached; keep the "best" one.
  public long itemId;
  public int itemRarity;

  public LootInfo(long lootId, String name, long itemId, int itemRarity) {
    this.lootId = lootId;
    this.name = name;
    this.itemId = itemId;
    this.itemRarity = itemRarity;
  }

  public Color getColor() {
    if (itemRarity < 0) {
      return ItemRarity.COMMON.color;
    }
    return ItemRarity.values()[itemRarity].color;
  }

  public boolean hasItemId() {
    return itemId >= 0;
  }

  public String getDatabaseURL() {
    if (itemId < 0) {
      return null;
    }
    return String.format("https://classic.wowhead.com/item=%d", itemId);
  }

  public InputStream getTooltipImage() throws Exception {
    if (itemId < 0) {
      return null;
    }
    String url = String.format("https://items.classicmaps.xyz/%d.png", itemId);  // TODO: Localize.
    InputStream file = new URL(url).openStream();
    return file;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (lootId ^ (lootId >>> 32));
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    LootInfo other = (LootInfo) obj;
    if (lootId != other.lootId)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return String.format("Loot<id=%d, name=%s, itemId=%d>", lootId, name, itemId);
  }

  public static String getDatabaseSearchURL(String query) throws UnsupportedEncodingException {
    return String.format("https://classic.wowhead.com/items/name:%s",
        URLEncoder.encode(query, Charset.defaultCharset().toString()));
  }

  private static LootInfo readNext(Cursor r) throws Exception {
    return new LootInfo(r.get("id", Long.class), r.get("name", String.class),
        r.getNullable("game_id", Long.class).orElse(-1L),
        r.getNullable("game_rarity", Integer.class).orElse(-1));
  }

  private static LootInfo get(Transaction tx, Statement q) throws Exception {
    try (Cursor r = q.executeFetch()) {
      if (r.next()) {
        return readNext(r);
      }
    }
    return null;
  }

  public static LootInfo getByLootId(Transaction tx, long lootId) throws Exception {
    try (Statement q = tx.prepare(String.format(GET_QUERY, "l.id = :key"))) {
      q.bind("key", lootId);
      return get(tx, q);
    }
  }

  public static LootInfo getByItemId(Transaction tx, long itemId) throws Exception {
    try (Statement q = tx.prepare(String.format(GET_QUERY, "lgi.game_id = :key"))) {
      q.bind("key", itemId);
      return get(tx, q);
    }
  }

  public static LootInfo getByName(Transaction tx, String name) throws Exception {
    try (Statement q = tx.prepare(String.format(GET_QUERY, "lower(l.name) = :key"))) {
      q.bind("key", name.toLowerCase());
      LootInfo out = get(tx, q);
      if (out != null) {
        return out;
      }
    }

    try (Statement q = tx.prepare(String.format(GET_ALIAS_QUERY, "lower(la.name) = :key"))) {
      q.bind("key", name.toLowerCase());
      LootInfo out = get(tx, q);
      if (out != null) {
        return out;
      }
    }

    return null;
  }

  public static LootInfo searchForSingleMatch(Transaction tx, String query) throws Exception {
    List<LootInfo> matches = search(tx, query);

    if (matches.isEmpty()) {
      return null;
    }

    if (matches.size() == 1) {
      return matches.get(0);
    }

    if (matches.get(0).name.toLowerCase().equals(query.toLowerCase())) {
      return matches.get(0);
    }

    return null;
  }

  public static List<LootInfo> search(Transaction tx, String query) throws Exception {
    if (query.matches("[0-9]+")) {
      // Assume query is an item ID if it is numeric.
      return ImmutableList.of(getByItemId(tx, Long.parseLong(query)));
    }

    Set<Long> ids = new HashSet<>();
    List<LootInfo> fuzzyMatches = new ArrayList<>();
    List<LootInfo> exactMatches = new ArrayList<>();

    query = query.replace("!", "!!").replace("%", "!%").replace("_", "!_").replace("[", "![");

    try (Statement q = tx.prepare(String.format(GET_ALIAS_QUERY, "la.name = :query"))) {
      q.bind("query", query);
      try (Cursor r = q.executeFetch()) {
        while (r.next()) {
          LootInfo out = readNext(r);
          if (!ids.contains(out.lootId)) {
            fuzzyMatches.add(out);
            ids.add(out.lootId);
          }
        }
      }
    }

    try (Statement q = tx.prepare(
        String.format(GET_QUERY, "l.name LIKE :query ESCAPE '!'"))) {
      q.bind("query", "%" + query + "%");
      try (Cursor r = q.executeFetch()) {
        while (r.next()) {
          LootInfo out = readNext(r);
          if (!ids.contains(out.lootId)) {
            if (query.toLowerCase().equals(out.name.toLowerCase())) {
              exactMatches.add(out);
            } else {
              fuzzyMatches.add(out);
            }
            ids.add(out.lootId);
          }
        }
      }
    }

    exactMatches.addAll(fuzzyMatches);
    return exactMatches;
  }
}
