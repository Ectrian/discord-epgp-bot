package com.epgpbot.transport.discord;

import java.io.File;
import java.io.FileReader;
import java.util.Set;

import com.epgpbot.config.Config;
import com.epgpbot.database.Database;
import com.epgpbot.database.mysql.SQLDatabase;
import com.epgpbot.transport.EventHandler;
import com.epgpbot.transport.Transport;
import com.epgpbot.util.JsonParser;
import com.google.common.base.Joiner;
import com.google.gson.FieldNamingPolicy;

/**
 * See Config.java for instructions.
 */
public class DiscordBotLauncher {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: epgpbot config.json");
      System.exit(1);
    }

    System.out.format("[Config] Loading... (%s)\n", args[0]);

    Config config = JsonParser
        .newJsonParser(FieldNamingPolicy.IDENTITY)
        .fromJson(new FileReader(new File(args[0])), Config.class);

    System.out.format("[Database] Creating pool... (%s@%s:%d)\n",
                      config.database_user,
                      config.database_host,
                      config.database_port);

    try (Database db = new SQLDatabase(
        config.database_host,
        config.database_port,
        config.database_user,
        config.database_password,
        config.database_name,
        config.database_use_tls
    )) {
      try (EventHandler handler = new EventHandler(db, config)) {
        try (Transport transport = new DiscordTransport(config.discord_bot_token)) {
          transport.run(handler);
        }
      }
    }

    System.out.format("[Thread] Transport thread quit.\n");

    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();

    for (Thread t : threadSet) {
      System.out.format("[Thread] Await: %s %s\n", t, Joiner.on(", ").join(t.getStackTrace()));
    }

    System.exit(0);
  }
}
