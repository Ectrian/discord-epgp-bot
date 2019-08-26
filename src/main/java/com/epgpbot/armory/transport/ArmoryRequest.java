package com.epgpbot.armory.transport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.epgpbot.armory.transport.ArmoryAPI.Guild;
import com.epgpbot.config.Config;
import com.epgpbot.database.Cursor;
import com.epgpbot.database.Database;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.database.mysql.SQLNull;

public class ArmoryRequest {
  private ArmoryRequestCallback callback;
  private List<String> charactersToSync;
  private List<String> playersToSync;
  private boolean syncAllCharacters;
  private boolean syncGuild;
  private boolean getOnlineGuildMembers;
  private boolean forced;
  private boolean returnCharacters;
  private boolean quit;

  public ArmoryRequest() {
    this.charactersToSync = new ArrayList<>();
    this.playersToSync = new ArrayList<>();
    this.syncAllCharacters = false;
    this.syncGuild = false;
    this.callback = null;
    this.getOnlineGuildMembers = false;
    this.forced = false;
    this.returnCharacters = false;
    this.quit = false;
  }

  public ArmoryRequest addCharacters(Collection<String> players) {
    this.charactersToSync.addAll(players);
    return this;
  }

  public ArmoryRequest addPlayers(Collection<String> players) {
    this.playersToSync.addAll(players);
    return this;
  }

  public ArmoryRequest setSyncGuild() {
    this.syncGuild = true;
    return this;
  }

  public ArmoryRequest setQuit() {
    this.quit = true;
    return this;
  }

  public ArmoryRequest setForced(boolean value) {
    this.forced = value;
    return this;
  }

  public ArmoryRequest setReturnCharacters(boolean value) {
    this.returnCharacters = value;
    return this;
  }

  public ArmoryRequest setSyncAllCharacters() {
    this.syncAllCharacters = true;
    return this;
  }

  public ArmoryRequest setCallback(ArmoryRequestCallback callback) {
    this.callback = callback;
    return this;
  }

  public boolean isQuit() {
    return this.quit;
  }

  protected void syncCharacter(ArmoryResponse rsp, Database db, ArmoryAPI api, String characterName)
      throws Exception {
    long currentTime = Instant.now().getEpochSecond();
    long lastSyncTime = 0;
    long characterId = -1;
    boolean isDeleted = false;

    try (Transaction tx = db.transaction()) {
      try (Statement q = tx.prepare(
          "SELECT id, last_armory_sync, deleted FROM characters WHERE lower(name) = :name;")) {
        q.bind("name", characterName.toLowerCase());
        try (Cursor r = q.executeFetch()) {
          if (r.next()) {
            characterId = r.get("id", Long.class);
            lastSyncTime = r.get("last_armory_sync", Long.class);
            isDeleted = r.get("deleted", Integer.class) != 0;
          }
        }
      }
    }

    rsp.totalCharacterCount++;

    if (!forced && (currentTime - lastSyncTime) < 60 * 60 * 12) {
      return;
    }

    rsp.updatedCharacterCount++;

    ArmoryAPI.Character character = api.getCharacter(characterName);
    if (character == null || character.name == null) {
      rsp.errors.add(String.format("Character '%s' does not exist.", characterName));
      if (characterId >= 0 && !isDeleted) {
        try (Transaction tx = db.transaction()) {
          try (Statement q = tx.prepare("UPDATE characters SET deleted = 1 WHERE id = :id;")) {
            q.bind("id", characterId);
            q.executeUpdate();
          }
        }
      }
      return;
    }
    if (returnCharacters) {
      rsp.characters.add(character);
    }

    try (Transaction tx = db.transaction()) {
      if (characterId < 0) {
        try (Statement q = tx.prepare("INSERT INTO characters (name) VALUES (:name);")) {
          q.bind("name", character.name);
          characterId = q.executeInsert();
        }
      }

      try (Statement q = tx.prepare("UPDATE characters " + "SET " + "  level = :level, "
          + "  race = :race, " + "  class = :class, " + "  guild_name = :guild_name, "
          + "  talent_spec1_tree = :talent_spec1_tree, "
          + "  talent_spec2_tree = :talent_spec2_tree, "
          + "  last_armory_sync = :last_armory_sync, " + "  deleted = 0 " + "WHERE id = :id;")) {

        q.bind("level", character.level);
        q.bind("race", character.race);
        q.bind("class", character.clazz);
        q.bind("guild_name", character.guild);

        if (character.talents != null && character.talents.size() >= 1) {
          q.bind("talent_spec1_tree", character.talents.get(0).tree);
        } else {
          q.bind("talent_spec1_tree", SQLNull.STRING);
        }

        if (character.talents != null && character.talents.size() >= 2) {
          q.bind("talent_spec2_tree", character.talents.get(1).tree);
        } else {
          q.bind("talent_spec2_tree", SQLNull.STRING);
        }

        q.bind("last_armory_sync", currentTime);
        q.bind("id", characterId);
        q.executeUpdate();
      }
    }
  }

  protected void syncPlayer(ArmoryResponse rsp, Database db, ArmoryAPI api, String playerName)
      throws Exception {
    List<String> characters = new ArrayList<>();

    try (Transaction tx = db.transaction()) {
      try (Statement q = tx.prepare("SELECT id FROM players WHERE name = :name;")) {
        q.bind("name", playerName.toLowerCase());
        try (Cursor r = q.executeFetch()) {
          if (!r.next()) {
            rsp.errors.add(String.format("Unknown player '%s'.", playerName));
          }
        }
      }

      try (Statement q = tx.prepare(
          "SELECT c.name FROM characters AS c JOIN players AS p ON c.player_id = p.id WHERE lower(p.name) = :name AND c.deleted = 0;")) {
        q.bind("name", playerName.toLowerCase());
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            characters.add(r.get("name", String.class));
          }
        }
      }
    }

    for (String characterName : characters) {
      syncCharacter(rsp, db, api, characterName);
    }
  }

  private void syncAllCharacters(ArmoryResponse rsp, Database db, ArmoryAPI api) throws Exception {
    List<String> characters = new ArrayList<>();

    try (Transaction tx = db.transaction()) {
      try (Statement q = tx.prepare("SELECT name FROM characters WHERE deleted = 0;")) {
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            characters.add(r.get("name", String.class));
          }
        }
      }
    }

    for (String characterName : characters) {
      syncCharacter(rsp, db, api, characterName);
    }
  }

  private void syncGuild(ArmoryResponse rsp, Database db, Config config, ArmoryAPI api)
      throws Exception {
    Guild g = api.getGuild(config.game_guild_name);
    if (g != null) {
      if (g.roster != null) {
        for (ArmoryAPI.Character c : g.roster) {
          if (c.name != null) {
            syncCharacter(rsp, db, api, c.name);
          }
        }
      }
    }
  }

  public void run(Database db, Config config, ArmoryAPI api) {
    ArmoryResponse rsp = new ArmoryResponse(this);

    try {
      if (syncGuild) {
        syncGuild(rsp, db, config, api);
      }

      if (getOnlineGuildMembers) {
        getOnlineGuildMembers(rsp, db, config, api);
      }

      if (syncAllCharacters) {
        syncAllCharacters(rsp, db, api);
      } else {
        for (String characterName : charactersToSync) {
          syncCharacter(rsp, db, api, characterName);
        }
        for (String playerName : playersToSync) {
          syncPlayer(rsp, db, api, playerName);
        }
      }

      rsp.didSucceed = rsp.errors.isEmpty();
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }

    if (callback != null) {
      callback.execute(rsp);
    }
  }

  private void getOnlineGuildMembers(ArmoryResponse rsp, Database db, Config config, ArmoryAPI api)
      throws Exception {
    Guild g = api.getGuild(config.game_guild_name);
    if (g != null) {
      if (g.roster != null) {
        for (ArmoryAPI.Character c : g.roster) {
          if (c.name != null && c.online) {
            if (returnCharacters) {
              rsp.characters.add(c);
            }
          }
        }
      }
    }
  }

  public ArmoryRequest setGetOnlineGuildMembers() {
    this.getOnlineGuildMembers = true;
    return this;
  }
}
