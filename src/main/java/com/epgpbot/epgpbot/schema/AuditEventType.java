package com.epgpbot.epgpbot.schema;

// IMPORTANT: DO NOT RE-ORDER (stored in DB).
public enum AuditEventType {
  CREATE_PLAYER,
  RENAME_PLAYER,
  RETROACTIVE_REWRITE,
  LINK_CHARACTER,
  LINK_TRANSPORT_ACCOUNT,
  MERGE_LOOT,
}
