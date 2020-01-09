package com.epgpbot.database;

public final class ArrayParameter<T> {
  private final String name;
  private final Class<T> type;

  ArrayParameter(String name, Class<T> type) {
    this.name = name;
    this.type = type;
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

  public static <T> ArrayParameter<T> declare(String name, Class<T> type) {
    return new ArrayParameter<T>(name, type);
  }
}
