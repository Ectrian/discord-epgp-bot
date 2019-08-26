package com.epgpbot.epgpbot.schema.game;

public enum Expansion {
  VANILLA(60),
  BURNING_CRUSADE(70),
  WRATH_OF_THE_LICH_KING(80),
  CATACLYSM(85),
  MISTS_OF_PANDARIA(90),
  WARLORDS_OF_DRAENOR(100),
  LEGION(110),
  BATTLE_FOR_AZEROTH(120);

  public final int maxCharacterLevel;

  private Expansion(int maxCharacterLevel) {
    this.maxCharacterLevel = maxCharacterLevel;
  }

  public static Expansion[] since(Expansion e) {
    Expansion[] out = new Expansion[Expansion.values().length - e.ordinal()];
    for (int i = 0; i < out.length; i++) {
      out[i] = Expansion.values()[e.ordinal() + i];
    }
    return out;
  }

  public static Expansion[] until(Expansion e) {
    Expansion[] out = new Expansion[e.ordinal()];
    for (int i = 0; i < out.length; i++) {
      out[i] = Expansion.values()[i];
    }
    return out;
  }
}
