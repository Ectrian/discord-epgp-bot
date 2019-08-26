package com.epgpbot.epgpbot.schema.game;

import java.awt.Color;

public enum ItemRarity {
  POOR("Poor", "#9d9d9d"),
  COMMON("Common", "#ffffff"),
  UNCOMMON("Uncommon", "#1eff00"),
  RARE("Rare", "#0070dd"),
  EPIC("Epic", "#a335ee"),
  LEGENDARY("Legendary", "#ff8000"),
  ARTIFACT("Artifact", "#e6cc80"),
  HEIRLOOM("Heirloom", "#e6cc80"),
  TOKEN("Token", "#00ccff");

  public final String name;
  public final Color color;

  private ItemRarity(String name, String color) {
    this.name = name;
    this.color = Color.decode(color);
  }
}
