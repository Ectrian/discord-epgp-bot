package com.epgpbot.epgpbot.schema;

import java.util.Arrays;

import com.google.common.base.Joiner;

// IMPORTANT: DO NOT RE-ORDER (stored in DB).
public enum EPGPEventType {
  IMPORT,
  MERGE,
  PENALTY,
  LOOT,
  STANDBY,
  RAID,
  INCENTIVE,
  DECAY,
  NEW_PLAYER,
  HIDDEN(false);

  public final boolean isDocumented;

  private EPGPEventType() {
    isDocumented = true;
  }

  private EPGPEventType(boolean isDocumented) {
    this.isDocumented = isDocumented;
  }

  public static EPGPEventType forString(String s) {
    for (EPGPEventType e : values()) {
      if (s.toLowerCase().equals(e.toString().toLowerCase())) {
        return e;
      }
    }
    return null;
  }

  public static String docChoices() {
    return Joiner.on("|").join(
        Arrays
          .stream(values())
          .filter(v -> v.isDocumented)
          .map(v -> v.toString().toLowerCase())
          .iterator());
  }
}
