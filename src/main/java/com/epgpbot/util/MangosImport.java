package com.epgpbot.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.DBField;
import com.epgpbot.database.Database;
import com.epgpbot.database.ScalarParameter;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.database.mysql.SQLDatabase;
import com.epgpbot.epgpbot.schema.game.CharacterClass;
import com.epgpbot.epgpbot.schema.game.Expansion;
import com.epgpbot.epgpbot.schema.game.ItemRarity;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

// Use Mangos Project DB since it already has items available in SQL format.
public class MangosImport {
  public static void main(String[] args) throws Exception {
      try (
        Database mangosDB = new SQLDatabase(
            "localhost",
            3306,
            "httpd",
            "httpd",
            "classicdb",
            false
        );
        Database botDB = new SQLDatabase(
            "localhost",
            3306,
            "httpd",
            "httpd",
            "epgpdb_aligned",
            false
        );
        Transaction mangosTx = mangosDB.transaction();
        Transaction botTx = botDB.transaction();
      ) {
        process(mangosTx, botTx);
      }
  }

  public static enum EquipSlot {
    NONE,
    HEAD,
    NECK,
    SHOULDERS,
    SHIRT,
    CHEST,
    WAIST,
    LEGS,
    FEET,
    WRISTS,
    HANDS,
    FINGER,
    TRINKET,
    WEAPON_1H, // treat as WEAPON
    SHIELD,
    RANGED, // treat as RANGED (bow)
    CLOAK,
    WEAPON_2H, // treat as WEAPON
    BAG,
    TABARD,
    ROBE,  // treat as CHEST (includes robes, probably for display reasons)
    WEAPON_MH, // treat as WEAPON
    WEAPON_OH, // treat as WEAPON
    HELD_IN_OH,
    AMMO,
    THROWN, // treat as RANGED
    RANGED2, // treat as RANGED (crossbow, gun, wand)
    QUIVER,
    RELIC; // treat as RANGED
  }

  public static enum ReputationRank {
    HATED,
    HOSTILE,
    UNFRIENDLY,
    NEUTRAL,
    FRIENDLY,
    HONORED,
    REVERED,
    EXALTED,
  }

  public static class ItemTemplate {
    static final Set<CharacterClass> EXPANSION_CLASSES = Arrays
        .stream(CharacterClass.values())
        .filter(c -> c.expansions.contains(Expansion.VANILLA))
        .collect(Collectors.toSet());
    public @DBField("entry") long id;
    public @DBField("name") String name;
    public @DBField("Quality") ItemRarity rarity;
    public @DBField("InventoryType") EquipSlot slot;
    public @DBField("displayid") long displayId;
    public @DBField("AllowableClass") long allowedClassesMask;
    public @DBField("description") String description;
    public @DBField("RequiredReputationRank") ReputationRank requiredReputation;
    public @DBField("RequiredLevel") int requiredLevel;
    public @DBField("ItemLevel") int itemLevel;

    public Set<CharacterClass> allowedClasses() {
      if (allowedClassesMask <= 0) {
        return ImmutableSet.of();
      }

      Set<CharacterClass> out = new HashSet<>();
      for (CharacterClass c : EXPANSION_CLASSES) {
        if ((allowedClassesMask & c.mask) != 0) {
          out.add(c);
        }
      }

      if (out.containsAll(EXPANSION_CLASSES)) {
        return ImmutableSet.of();
      }

      return out;
    }

    @Override
    public String toString() {
      return String.format(
        "ItemTemplate{id=%d, name=%s, rarity=%s, slot=%s, displayId=%d, rep=%s, level=%d, classes=%d:%s}",
        id,
        name,
        rarity,
        slot,
        displayId,
        requiredReputation,
        requiredLevel,
        allowedClassesMask,
        allowedClasses()
      );
    }
  }

  private static void process(Transaction mangosTx, Transaction botTx) throws Exception {
    Map<Long, String> nameOverrides = null; // calculateNameOverrides(mangosTx);

    try (Statement q = mangosTx.prepare("SELECT * FROM item_template ORDER BY name ASC;")) {
      try (Cursor r = q.executeFetch()) {
        for (ItemTemplate item : r.iterate(ItemTemplate::new)) {
          processItem(botTx, nameOverrides, item);
        }
      }
    }
  }

  private static Map<Long, String> calculateNameOverrides(Transaction mangosTx) throws Exception {
    Map<Long, String> out = new HashMap<>();

    out.put(18564L, "Bindings of the Windseeker (Right)");
    out.put(18563L, "Bindings of the Windseeker (Left)");
    out.put(18423L, "Head of Onyxia (A)");
    out.put(18422L, "Head of Onyxia (H)");
    out.put(19003L, "Head of Nefarian (A)");
    out.put(19002L, "Head of Nefarian (H)");
    out.put(19017L, "Essence of the Firelord");
    out.put(18566L, null);

    try (Statement q = mangosTx.prepare("SELECT name FROM item_template GROUP BY name HAVING COUNT(entry) > 1;")) {
      try (Cursor r = q.executeFetch()) {
        while (r.next()) {
          calculateNameOverride(mangosTx, out, r.get("name", String.class));
        }
      }
    }
    return out;
  }

  @FunctionalInterface
  interface NamingStrategy {
    public String apply(ItemTemplate item);
  }

  private static boolean tryNamingStrategy(List<ItemTemplate> conflicts, Map<Long, String> out, NamingStrategy strategy) {
    Set<String> uniqueNames = new HashSet<>();

    for (ItemTemplate item : conflicts) {
      String uniqueName = strategy.apply(item);
      if (!uniqueNames.add(uniqueName)) {
        return false;
      }
    }

    for (ItemTemplate item : conflicts) {
      if (!out.containsKey(item.id)) {
        out.put(item.id, strategy.apply(item));
      }
    }

    return true;
  }

  private static void calculateNameOverride(Transaction mangosTx, Map<Long, String> out, String name) throws Exception {
    System.out.format("Calculating unambiguous names for '%s'\n", name);
    final ScalarParameter<String> P_NAME = ScalarParameter.declare("name", String.class);
    List<ItemTemplate> conflicts;

    try (Statement q = mangosTx.prepare("SELECT * FROM item_template WHERE name = ", P_NAME, ";")) {
      q.bind(P_NAME, name);
      try (Cursor r = q.executeFetch()) {
        conflicts = r.toList(ItemTemplate::new);
      }
    }

    if (tryNamingStrategy(conflicts, out, (item) -> String.format("%s (%d)", item.name, item.requiredLevel))) {
      return;
    }
    if (tryNamingStrategy(conflicts, out, (item) -> String.format("%s (%s)", item.name, Joiner.on(", ").join(item.allowedClasses())))) {
      return;
    }
    if (tryNamingStrategy(conflicts, out, (item) -> String.format("%s (%s)", item.name, item.requiredReputation))) {
      return;
    }
    if (tryNamingStrategy(conflicts, out, (item) -> String.format("%s (%s)", item.name, item.slot))) {
      return;
    }
    if (tryNamingStrategy(conflicts, out, (item) -> String.format("%s (%s)", item.name, item.rarity))) {
      return;
    }
    if (tryNamingStrategy(conflicts, out, (item) -> String.format("%s (%s %d)", item.name, item.rarity, item.itemLevel))) {
      return;
    }
    if (tryNamingStrategy(conflicts, out, (item) -> String.format("%s (ID: %d)", item.name, item.id))) {
      return;
    }

    throw new IllegalStateException(String.format("Failed to disambiguate name '%s'", name));
  }

  private static void processItem(Transaction botTx, Map<Long, String> nameOverrides, ItemTemplate item) throws Exception {
    ScalarParameter<Long> P_ID = ScalarParameter.declare("id", Long.class);
    ScalarParameter<Integer> P_SLOT = ScalarParameter.declare("slot", Integer.class);

    /*
    if (nameOverrides.containsKey(item.id)) {
      String override = nameOverrides.get(item.id);
      if (override == null) {
        return;
      }
      if (override.contains("ID:")) {
        System.out.format("DISAMBIGUATE: %d : %s -> %s\n", item.id, item.name, override);
      }
    }
    */

    try (Statement q = botTx.prepare("UPDATE loot_game_info SET game_slot = ", P_SLOT, " WHERE game_id = ", P_ID, ";")) {
      q.bind(P_SLOT, item.slot.ordinal());
      q.bind(P_ID, item.id);
      q.executeUpdate();
    }
  }
}
