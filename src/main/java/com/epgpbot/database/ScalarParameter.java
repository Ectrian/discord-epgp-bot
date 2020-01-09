package com.epgpbot.database;

public final class ScalarParameter<T> {
  private final String name;
  private final Class<T> type;

  ScalarParameter(String name, Class<T> type) {
    this.name = name;
    this.type = type;
  }

  public static <T> ScalarParameter<T> declare(String name, Class<T> type) {
    return new ScalarParameter<T>(name, type);
  }

  public String name() {
    return name;
  }

  public Class<T> type() {
    return type;
  }

  @Override
  public String toString() {
    return ":" + name;
  }
}