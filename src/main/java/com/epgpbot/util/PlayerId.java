package com.epgpbot.util;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;

public class PlayerId {
  public final long id;
  public final long characterId;
  public final String name;
  public final String transportUserId;

  public PlayerId(long id, String name, String transportUserId) {
    this.id = id;
    this.name = name;
    this.transportUserId = transportUserId;
    this.characterId = -1;
  }

  public PlayerId(long id, long characterId, String name, String transportUserId) {
    this.id = id;
    this.name = name;
    this.transportUserId = transportUserId;
    this.characterId = characterId;
  }

  public static PlayerId withTransportInfo(Transaction tx, long id, long characterId, String name) throws Exception {
    try (Statement q = tx.prepare("SELECT id FROM transport_users WHERE player_id = :id;")) {
      q.bind("id", id);
      try (Cursor r = q.executeFetch()) {
        if (!r.next()) {
          return new PlayerId(id, characterId, name, null);
        }
        return new PlayerId(id, characterId, name, r.get("id", String.class));
      }
    }
  }
}