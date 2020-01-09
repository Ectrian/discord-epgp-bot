package com.epgpbot.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import com.epgpbot.config.Config;
import com.epgpbot.database.Cursor;
import com.epgpbot.database.Database;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.database.mysql.SQLDatabase;
import com.epgpbot.epgpbot.commands.CommandHandlerAbstract;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.google.gson.FieldNamingPolicy;

public class LootImport {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new IllegalArgumentException("Usage: loot.import config.json file.csv");
    }

    Config config = JsonParser
        .newJsonParser(FieldNamingPolicy.IDENTITY)
        .fromJson(new FileReader(new File(args[0])), Config.class);

    File csvFile = new File(args[1]);

    try (Database db = new SQLDatabase(
        config.database_host,
        config.database_port,
        config.database_user,
        config.database_password,
        config.database_name,
        config.database_use_tls
    )) {
      try (Transaction tx = db.transaction()) {
        handleImport(csvFile, tx);
      }
    }
  }

  static class CSVEntry {
    String person;
    String item;
    String date;

    private CSVEntry() {}

    public static CSVEntry fromLine(String line) {
      String[] parts = line.split(",");

      if (parts.length != 3) {
        throw new IllegalArgumentException(line);
      }

      CSVEntry entry = new CSVEntry();
      entry.person = parts[0].trim();
      entry.item = parts[1].trim();
      entry.date = parts[2].trim();
      return entry;
    }

    public LootInfo getLoot(Transaction tx) throws Exception {
      LootInfo loot = LootInfo.getByName(tx, item);
      if (loot == null) {
        throw new IllegalArgumentException(item);
      }
      return loot;
    }

    public PlayerId getPlayer(Transaction tx) throws Exception {
      Argument arg = new Argument(null, person);
      return arg.characterPlayerIdValue(tx);
    }

    public long getDate() throws Exception {
      SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy hh:mm a zzz");
      Date time = fmt.parse(date + " 8:00 PM PST");
      return time.toInstant().getEpochSecond();
    }

    @Override
    public String toString() {
      return String.format("Entry[person=%s, item=%s, date=%s]",
          person, item, date);
    }

    public void writeToDatabase(Transaction tx) throws Exception {
      PlayerId player = getPlayer(tx);
      long characterId = (new Argument(null, person)).characterIdValue(tx);
      LootInfo loot = getLoot(tx);
      long date = getDate();

      try (Statement q = tx.prepare("SELECT * FROM epgp_log WHERE loot_id = :loot_id AND target_player_id = :player_id;")) {
        q.bind("loot_id", loot.lootId);
        q.bind("player_id", player.id);
        try (Cursor r = q.executeFetch()) {
          boolean any = false;
          long recordedDate = 0;

          while (r.next()) {
            any = true;
            recordedDate = r.get("timestamp", Long.class);
            if (Math.abs(date - recordedDate) < 60 * 60 * 6) {
              System.out.format("Record already in database - skipping (%s).\n", this);
              return;
            }
          }

          if (any) {
            if (!player.name.equals("Disenchant") &&
                !player.name.equals("GBank")) {
              System.out.format("Record already in database, but dates differ (%s).\n", this);
              System.out.format("  csv date: %s\n", CommandHandlerAbstract.formatDate(date));
              System.out.format("  db date: %s\n", CommandHandlerAbstract.formatDate(recordedDate));
              throw new IllegalStateException();
            }
          }
        }

        System.out.format("Record not in database - adding (%s).\n", this);
        addLogEntry(tx, player.id, characterId, loot.lootId, 1, date);
      }
    }

    protected void addLogEntry(
        Transaction tx,
        long targetPlayerId,
        long targetCharacterId,
        long lootId,
        long gpDelta,
        long timestamp) throws Exception {
      try (
        Statement q = tx.prepare(
              "INSERT INTO epgp_log ("
            + "timestamp, target_player_id, target_character_id, source_player_id, type, loot_id, gp_delta, action_timestamp"
            + ") VALUES ("
            + ":timestamp, :target_player_id, :target_character_id, :source_player_id, :type, :loot_id, :gp_delta, :action_timestamp"
            + ");")
      ) {
        q.bind("action_timestamp", Instant.now().getEpochSecond());
        q.bind("timestamp", timestamp);
        q.bind("target_player_id", targetPlayerId);
        q.bind("target_character_id", targetCharacterId);
        q.bind("source_player_id", (new Argument(null, "Ectrian")).playerIdValue(tx).id);
        q.bind("type", EPGPEventType.LOOT.ordinal());
        q.bind("loot_id", lootId);
        q.bind("gp_delta", 1);
        q.executeInsert();
      }

      try (Statement q = tx.prepare(
          "UPDATE players "
        + "SET ep_earned = ep_earned + :ep, gp_earned = gp_earned + :gp, gp_net = gp_net + :gp, ep_net = ep_net + :ep "
        + "WHERE id = :player_id;")) {
        q.bind("gp", 1);
        q.bind("ep", 0);
        q.bind("player_id", targetPlayerId);
        q.executeUpdate();
      }
    }
  }

  private static void handleImport(File csvFile, Transaction tx) throws Exception {
    try (
      FileReader fileReader = new FileReader(csvFile);
      BufferedReader bufReader = new BufferedReader(fileReader);
    ) {
      String line;
      while ((line = bufReader.readLine()) != null) {
        CSVEntry entry = CSVEntry.fromLine(line);
        // System.out.format("LINE: %s\n", entry);
        // System.out.format("Loot: %s\n", entry.getLoot(tx));
        // System.out.format("Player: %s\n", entry.getPlayer(tx));
        // System.out.format("Date: %d %s\n", entry.getDate(), CommandHandlerAbstract.formatDate(entry.getDate()));
        entry.writeToDatabase(tx);
      }
    }
  }
}
