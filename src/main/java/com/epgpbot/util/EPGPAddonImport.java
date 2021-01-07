package com.epgpbot.util;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.List;

import com.epgpbot.config.Config;
import com.epgpbot.database.Cursor;
import com.epgpbot.database.Database;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.database.mysql.SQLDatabase;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.google.common.base.Joiner;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * Imports data from the export functionality of the EPGP Add-On.
 * e.g. /epgp -> Log -> Export
 */
public class EPGPAddonImport {
  public static class RosterEntry {
    public String main;
    public int ep;
    public int gp;
    public List<String> alts;

    @Override
    public String toString() {
      return String.format("RosterEntry<%s, %d, %d, %s>", main, ep, gp, alts);
    }
  }

  public static class RosterEntryDeserializer implements JsonDeserializer<RosterEntry> {
    @Override
    public RosterEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonArray data = json.getAsJsonArray();

      if (data.size() != 4 && data.size() != 3) {
        throw new JsonParseException("Unable to deserialize roster entry");
      }

      RosterEntry out = new RosterEntry();
      out.main = data.get(0).getAsString();
      out.ep = data.get(1).getAsInt();
      out.gp = data.get(2).getAsInt();
      if (data.size() >= 4) {
        // Note: only the modified add-on will include alts
        try {
          out.alts = context.deserialize(data.get(3), RosterEntry.class.getField("alts").getType());
        } catch (SecurityException | NoSuchFieldException e) {
          throw new JsonParseException(e);
        }
      }
      return out;
    }
  }

  public static class EPGPData {
    public String region;
    public String realm;
    public String guild;
    public int min_ep;
    public int base_gp;
    public int decay_p;
    public int extras_p;
    public long timestamp;
    public RosterEntry roster[];
  }

  static void ASSERT(boolean condition, Object ...args) {
    if (!condition) {
      throw new IllegalStateException(Joiner.on(", ").join(args));
    }
  }

  public static class DBWrapper {
    protected final Transaction tx;

    public DBWrapper(Transaction tx) {
      this.tx = tx;
    }

    public void setEPGP(long playerId, int ep, int gp) throws Exception {
      try (Statement q = tx.prepare(
          "UPDATE players ",
          "SET ",
          "  ep_earned = :ep_earned, ",
          "  gp_earned = :gp_earned, ",
          "  ep_net = :ep_net, ",
          "  gp_net = :gp_net ",
          "WHERE id = :id;"
        )) {
        q.bind("id", playerId);
        q.bind("ep_net", ep);
        q.bind("ep_earned", ep);
        q.bind("gp_net", gp);
        q.bind("gp_earned", gp);
        q.executeUpdate();
      }
    }

    public long getOrCreatePlayer(String name) throws Exception {
      try (Statement q = tx.prepare("SELECT id FROM players WHERE lower(name) = :name;")) {
        q.bind("name", name.toLowerCase());
        try (Cursor r = q.executeFetch()) {
          if (r.next()) {
            return r.get("id", Long.class);
          }
        }
      }

      return createPlayer(name);
    }

    public long createPlayer(String name) throws Exception {
      try (Statement q = tx.prepare("INSERT INTO players (name) VALUES (:name);")) {
        q.bind("name", name);
        return q.executeInsert();
      }
    }

    public void assignCharacter(long playerId, long characterId) throws Exception {
      try (Statement q = tx.prepare("UPDATE characters SET player_id = :player_id WHERE id = :id;")) {
        q.bind("player_id", playerId);
        q.bind("id", characterId);
        q.executeUpdate();
      }
    }

    public long getOrCreateCharacter(long playerId, String name) throws Exception {
      try (Statement q = tx.prepare("SELECT id, player_id FROM characters WHERE lower(name) = :name;")) {
        q.bind("name", name.toLowerCase());
        try (Cursor r = q.executeFetch()) {
          if (r.next()) {
            long characterId = r.get("id", Long.class);
            assignCharacter(playerId, characterId);
            return characterId;
          }
        }
      }

      return createCharacter(playerId, name);
    }

    public long createCharacter(long playerId, String name) throws Exception {
      try (Statement u = tx.prepare("INSERT INTO characters (name, player_id) VALUES (:name, :player_id);")) {
        u.bind("name", name);
        u.bind("player_id", playerId);
        return u.executeInsert();
      }
    }

    public void createImportLogEntry(long playerId, long characterId, long timestamp, int ep, int gp) throws Exception {
      try (Statement u = tx.prepare(
          "INSERT INTO epgp_log (",
          "  timestamp, ",
          "  action_timestamp, ",
          "  target_player_id, ",
          "  target_character_id, ",
          "  source_player_id, ",
          "  type, ",
          "  ep_delta, ",
          "  gp_delta, ",
          "  note",
          ") VALUES (",
          "  :timestamp, ",
          "  :action_timestamp, ",
          "  :target_player_id, ",
          "  :target_character_id, ",
          "  :source_player_id, ",
          "  :type, ",
          "  :ep_delta, ",
          "  :gp_delta, ",
          "  :note",
          ");"
      )) {
        u.bind("timestamp", timestamp);
        u.bind("action_timestamp", timestamp);
        u.bind("target_player_id", playerId);
        u.bind("target_character_id", characterId);
        u.bind("source_player_id", playerId);
        u.bind("type", EPGPEventType.IMPORT.ordinal());
        u.bind("ep_delta", ep);
        u.bind("gp_delta", gp);
        u.bind("note", "Import from in-game addon.");
        u.executeUpdate();
      }
    }


    public void wipeLog(long playerId) throws Exception {
      try (Statement q = tx.prepare("DELETE FROM epgp_log WHERE target_player_id = :target_player_id;")) {
        q.bind("target_player_id", playerId);
        q.executeUpdate();
      }
    }
  }

  public static void doImport(Transaction tx, Config config, EPGPData data) throws Exception {
    ASSERT(config.epgp_base_gp == data.base_gp, config.epgp_base_gp, data.base_gp);
    ASSERT(config.epgp_decay_base_gp == data.base_gp, config.epgp_decay_base_gp, data.base_gp);
    ASSERT(config.epgp_decay_rate == data.decay_p, config.epgp_decay_rate, data.decay_p);
    ASSERT(config.game_realm_name.equals(data.realm), config.game_realm_name, data.realm);
    ASSERT(config.game_guild_name.equals(data.guild), config.game_guild_name, data.guild);

    DBWrapper db = new DBWrapper(tx);

    for (RosterEntry e : data.roster) {
      System.out.format("Import: %s\n", e);

      long playerId = db.getOrCreatePlayer(e.main);
      long mainCharacterId = db.getOrCreateCharacter(playerId, e.main);
      for (String alt : e.alts) {
        db.getOrCreateCharacter(playerId, alt);
      }

      db.wipeLog(playerId);
      db.setEPGP(playerId, e.ep, e.gp - data.base_gp);
      db.createImportLogEntry(playerId, mainCharacterId, data.timestamp, e.ep, e.gp - data.base_gp);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.println("Usage: import config.json data.json");
      System.exit(1);
    }

    Gson json = JsonParser
        .newJsonParserBuilder(FieldNamingPolicy.IDENTITY)
        .registerTypeAdapter(RosterEntry.class, new RosterEntryDeserializer())
        .create();
    Config config = json.fromJson(new FileReader(new File(args[0])), Config.class);
    EPGPData data = json.fromJson(new FileReader(new File(args[1])), EPGPData.class);

    try (Database db = new SQLDatabase(
        config.database_host,
        config.database_port,
        config.database_user,
        config.database_password,
        config.database_name,
        config.database_use_tls
    )) {
      try (Transaction tx = db.transaction()) {
        try {
          doImport(tx, config, data);
        } catch (Throwable e) {
          tx.abort();
          throw e;
        }
      }
    }
  }
}
