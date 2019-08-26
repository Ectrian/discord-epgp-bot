package com.epgpbot.database;

public enum IsolationLevel {
  SERIALIZABLE,
  REPEATABLE_READ,
  READ_UNCOMMITTED,
  READ_COMMITTED,
  DEFAULT;
}
