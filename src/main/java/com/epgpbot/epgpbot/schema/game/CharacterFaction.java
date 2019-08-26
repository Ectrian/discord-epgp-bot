package com.epgpbot.epgpbot.schema.game;

public enum CharacterFaction {
  ALLIANCE("Alliance"),
  HORDE("Horde");

  public final String name;

  private CharacterFaction(String name) {
    this.name = name;
  }
}
