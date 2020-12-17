package com.epgpbot.epgpbot.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;

public class MergedLogEntry {
  public final List<LogEntry> entries;

  private MergedLogEntry() {
    this.entries = new ArrayList<>();
  }

  private MergedLogEntry(LogEntry entry) {
    this();
    add(entry);
  }

  private MergedLogEntry(List<LogEntry> entries) {
    this.entries = entries;
  }

  public int size() {
    return entries.size();
  }

  public void add(LogEntry entry) {
    entries.add(entry);
  }

  public LogEntry head() {
    return entries.get(0);
  }

  private Map<String, Object> formatRow(LogEntryFormatOptions opts) {
    Map<String, Object> row = new HashMap<>();
    LogEntry first = head();
    Map<String, Integer> targetPlayerNames = new LinkedHashMap<>();
    Map<String, Integer> targetCharacterNames = new LinkedHashMap<>();

    for (LogEntry entry : entries) {
      if (!targetPlayerNames.containsKey(entry.targetPlayerName)) {
        targetPlayerNames.put(entry.targetPlayerName, 0);
      }
      targetPlayerNames.put(entry.targetPlayerName, targetPlayerNames.get(entry.targetPlayerName) + 1);

      if (entry.targetCharacterName.isPresent()) {
        String targetCharacterName = entry.targetCharacterName.get();
        if (!targetCharacterNames.containsKey(targetCharacterName)) {
          targetCharacterNames.put(targetCharacterName, 0);
        }
        targetCharacterNames.put(targetCharacterName, targetCharacterNames.get(targetCharacterName) + 1);
      }
    }

    if (opts.showEventId) {
      if (first.eventType == EPGPEventType.DECAY && entries.size() > 1) {
        row.put("id", "*");
      } else {
        Iterator<Long> eventIds = entries.stream().map((e) -> e.eventId).iterator();
        row.put("id", Joiner.on(", ").join(eventIds));
      }
    }

    List<String> playerNames = new ArrayList<>();
    for (Map.Entry<String, Integer> entry : targetPlayerNames.entrySet()) {
      if (entry.getValue() == 1) {
        playerNames.add(entry.getKey());
      } else {
        playerNames.add(String.format("%s (x%d)", entry.getKey(), entry.getValue()));
      }
    }
    row.put("player", Joiner.on(", ").join(playerNames));

    if (opts.showTargetCharacterName) {
      List<String> characterNames = new ArrayList<>();
      for (Map.Entry<String, Integer> entry : targetCharacterNames.entrySet()) {
        if (entry.getValue() == 1) {
          characterNames.add(entry.getKey());
        } else {
          characterNames.add(String.format("%s (x%d)", entry.getKey(), entry.getValue()));
        }
      }
      row.put("character", Joiner.on(", ").join(characterNames));
    }

    row.put("date", first.getFormattedDate());
    row.put("officer", first.sourcePlayerName);

    if (first.epDelta != 0) {
      if (first.epDelta > 0) {
        row.put("ep", String.format("+%d", first.epDelta));
      } else {
        row.put("ep", first.epDelta);
      }
    }

    if (first.gpDelta != 0) {
      if (first.gpDelta > 0) {
        row.put("gp", String.format("+%d", first.gpDelta));
      } else {
        row.put("gp", first.gpDelta);
      }
    }

    if (opts.showNote &&
        first.note.isPresent() &&
        first.eventType != EPGPEventType.PENALTY &&
        first.eventType != EPGPEventType.INCENTIVE) {
      row.put("note", first.note.get());
    }

    if (opts.showSlot && first.slot.isPresent()) {
      row.put("slot", first.slot);
    }

    if (first.eventType == EPGPEventType.DECAY) {
      if (targetPlayerNames.size() > 1) {
        row.put("ep", "*");
        row.put("gp", "*");
        row.put("player", String.format("(%d players)", targetPlayerNames.size()));
      }
      if (opts.showTargetCharacterName) {
        row.put("character", "*");
      }
      row.put("reason", "Decay");
    }

    else if (first.eventType == EPGPEventType.PENALTY) {
      row.put("reason", String.format("Penalty (%s)", first.note.orElse("UNKNOWN REASON")));
    }

    else if (first.eventType == EPGPEventType.INCENTIVE) {
      row.put("reason", String.format("Incentive (%s)", first.note.orElse("UNKNOWN REASON")));
    }

    else if (first.eventType == EPGPEventType.LOOT) {
      row.put("reason", String.format("Loot (%s)", first.lootName.orElse("UNKNOWN ITEM")));
    }

    else if (first.eventType == EPGPEventType.RAID) {
      row.put("reason", String.format("Raid (%s)", (first.raidType.isPresent() ? first.raidType.get() : "UNKNOWN RAID")));
    }

    else if (first.eventType == EPGPEventType.STANDBY) {
      row.put("reason", String.format("Standby (%s)", (first.raidType.isPresent() ? first.raidType.get() : "UNKNOWN RAID")));
    }

    else if (first.eventType == EPGPEventType.HIDDEN) {
      row.put("reason", String.format("Hidden"));
    }

    else if (first.eventType == EPGPEventType.EQUIP) {
      row.put("reason", String.format("Equip (%s)", first.lootName.orElse("UNKNOWN ITEM")));
    }

    else {
      row.put("reason", first.eventType);
    }

    if (first.undoes.isPresent()) {
      row.put("reason", String.format("%s (UNDO)", row.get("reason")));
    }
    if (first.undoneBy.isPresent()) {
      row.put("reason", String.format("%s*", row.get("reason")));
    }

    return row;
  }

  public static List<MergedLogEntry> fromEntries(List<LogEntry> entries, LogEntryFormatOptions opts) {
    List<MergedLogEntry> out = new ArrayList<>();
    MergedLogEntry lastEntry = null;

    for (LogEntry entry : entries) {
      if (lastEntry != null) {
        if (entry.canMergeInto(lastEntry, opts)) {
          lastEntry.add(entry);
          continue;
        }
      }

      lastEntry = new MergedLogEntry(entry);
      out.add(lastEntry);
    }

    return out;
  }

  public static List<Map<String, Object>> formatTable(List<MergedLogEntry> entries, LogEntryFormatOptions opts) {
    List<Map<String, Object>> result = new ArrayList<>();

    for (MergedLogEntry entry : entries) {
      result.add(entry.formatRow(opts));
    }

    return result;
  }
}