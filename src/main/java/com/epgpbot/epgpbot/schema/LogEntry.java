package com.epgpbot.epgpbot.schema;

import java.util.Optional;

import com.epgpbot.database.Cursor;
import com.epgpbot.epgpbot.commands.CommandHandlerAbstract;

public class LogEntry {
  public long eventId;
  public long timestamp;
  public long targetPlayerId;
  public String targetPlayerName;
  public Optional<Long> targetCharacterId;
  public Optional<String> targetCharacterName;
  public long sourcePlayerId;
  public String sourcePlayerName;
  public EPGPEventType eventType;
  public Optional<RaidType> raidType;
  public Optional<Long> lootId;
  public Optional<String> lootName;
  public long epDelta;
  public long gpDelta;
  public Optional<String> note;
  public Optional<Long> undoneBy;
  public Optional<Long> undoes;

  public String getFormattedDate() {
    return CommandHandlerAbstract.formatDate(timestamp);
  }

  public static LogEntry readNext(Cursor r) throws Exception {
    LogEntry entry = new LogEntry();
    entry.eventId = r.get("id", Long.class);
    entry.timestamp = r.get("timestamp", Long.class);
    entry.targetPlayerId = r.get("target_player_id", Long.class);
    entry.targetPlayerName = r.get("target_player_name", String.class);
    entry.sourcePlayerId = r.get("source_player_id", Long.class);
    entry.sourcePlayerName = r.get("source_player_name", String.class);
    entry.targetCharacterId = r.getNullable("target_character_id", Long.class);
    entry.targetCharacterName = r.getNullable("target_character_name", String.class);
    entry.eventType = EPGPEventType.values()[r.get("type", Integer.class)];
    entry.epDelta = r.get("ep_delta", Long.class);
    entry.gpDelta = r.get("gp_delta", Long.class);
    entry.note = r.getNullable("note", String.class);
    entry.lootName = r.getNullable("loot_name", String.class);
    entry.lootId = r.getNullable("loot_id", Long.class);
    entry.raidType = r.getNullable("raid_type", Integer.class).map((i) -> RaidType.values()[i]);
    entry.undoes = r.getNullable("undoes", Long.class);
    entry.undoneBy = r.getNullable("undone_by", Long.class);
    return entry;
  }

  public boolean canMergeInto(MergedLogEntry mergedEntry, LogEntryFormatOptions opts) {
    if (mergedEntry.size() == 0) {
      return true;
    }

    LogEntry other = mergedEntry.head();

    if (this.eventType == EPGPEventType.DECAY &&
        this.eventType == other.eventType &&
        this.sourcePlayerId == other.sourcePlayerId &&
        Math.abs(other.timestamp - this.timestamp) < 600) {
      return true;
    }

    if (opts.showEventId || opts.showTargetCharacterName) {
      return false;
    }

    if (mergedEntry.size() >= 10) {
      return false;  // Keep row size small.
    }

    return (!other.undoneBy.isPresent()) &&
           (!this.undoneBy.isPresent()) &&
           (!other.undoes.isPresent()) &&
           (!this.undoes.isPresent()) &&
           (other.epDelta == this.epDelta) &&
           (other.gpDelta == this.gpDelta) &&
           (other.note.equals(this.note)) &&
           (other.raidType.equals(this.raidType)) &&
           (other.lootId.equals(this.lootId)) &&
           (other.eventType == this.eventType) &&
           (other.sourcePlayerId == this.sourcePlayerId) &&
           Math.abs(other.timestamp - this.timestamp) < 600;
  }
}