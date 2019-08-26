package com.epgpbot.epgpbot.schema.game;

import com.google.common.collect.ImmutableSet;

public enum CharacterRace {
  DRAENEI(
      "Draenei",
      new CharacterFaction[]{CharacterFaction.ALLIANCE},
      Expansion.since(Expansion.BURNING_CRUSADE)
  ),
  DWARF(
      "Dwarf",
      new CharacterFaction[]{CharacterFaction.ALLIANCE},
      Expansion.since(Expansion.VANILLA)
  ),
  GNOME(
      "Gnome",
      new CharacterFaction[]{CharacterFaction.ALLIANCE},
      Expansion.since(Expansion.VANILLA)
  ),
  HUMAN(
      "Human",
      new CharacterFaction[]{CharacterFaction.ALLIANCE},
      Expansion.since(Expansion.VANILLA)
  ),
  NIGHT_ELF(
      "Night Elf",
      new CharacterFaction[]{CharacterFaction.ALLIANCE},
      Expansion.since(Expansion.VANILLA)
  ),
  WORGEN(
      "Worgen",
      new CharacterFaction[]{CharacterFaction.ALLIANCE},
      Expansion.since(Expansion.CATACLYSM)
  ),
  VOID_ELF(
      "Void Elf",
      new CharacterFaction[]{CharacterFaction.ALLIANCE},
      Expansion.since(Expansion.LEGION)
  ),
  LIGHTFORGED_DRAENEI(
      "Lightforged Draenei",
      new CharacterFaction[]{CharacterFaction.ALLIANCE},
      Expansion.since(Expansion.LEGION)
  ),
  DARK_IRON_DWARF(
      "Dark Iron Dwarf",
      new CharacterFaction[]{CharacterFaction.ALLIANCE},
      Expansion.since(Expansion.LEGION)
  ),
  KUL_TIRAN_HUMAN(
      "Kul Tiran Human",
      new CharacterFaction[]{CharacterFaction.ALLIANCE},
      Expansion.since(Expansion.BATTLE_FOR_AZEROTH)
  ),
  PANDAREN(
      "Pandaren",
      new CharacterFaction[]{CharacterFaction.ALLIANCE, CharacterFaction.HORDE},
      Expansion.since(Expansion.MISTS_OF_PANDARIA)
  ),
  BLOOD_ELF(
      "Blood Elf",
      new CharacterFaction[]{CharacterFaction.HORDE},
      Expansion.since(Expansion.BURNING_CRUSADE)
  ),
  GOBLIN(
      "Goblin",
      new CharacterFaction[]{CharacterFaction.HORDE},
      Expansion.since(Expansion.CATACLYSM)
  ),
  ORC(
      "Orc",
      new CharacterFaction[]{CharacterFaction.HORDE},
      Expansion.since(Expansion.VANILLA)
  ),
  TAUREN(
      "Tauren",
      new CharacterFaction[]{CharacterFaction.HORDE},
      Expansion.since(Expansion.VANILLA)
  ),
  TROLL(
      "Troll",
      new CharacterFaction[]{CharacterFaction.HORDE},
      Expansion.since(Expansion.VANILLA)
  ),
  UNDEAD(
      "Undead",
      new CharacterFaction[]{CharacterFaction.HORDE},
      Expansion.since(Expansion.VANILLA)
  ),
  NIGHTBORNE(
      "Nightborne",
      new CharacterFaction[]{CharacterFaction.HORDE},
      Expansion.since(Expansion.LEGION)
  ),
  HIGHMOUNTAIN_TAUREN(
      "Highmountain Tauren",
      new CharacterFaction[]{CharacterFaction.HORDE},
      Expansion.since(Expansion.LEGION)
  ),
  MAGHAR(
      "Mag'har",
      new CharacterFaction[]{CharacterFaction.HORDE},
      Expansion.since(Expansion.LEGION)
  ),
  ZANDALARI(
      "Zandalari",
      new CharacterFaction[]{CharacterFaction.HORDE},
      Expansion.since(Expansion.BATTLE_FOR_AZEROTH)
  );

  public final String name;
  public final ImmutableSet<CharacterFaction> factions;
  public final ImmutableSet<Expansion> expansions;

  private CharacterRace(String name, CharacterFaction[] factions, Expansion[] expansions) {
    this.name = name;
    this.factions = ImmutableSet.copyOf(factions);
    this.expansions = ImmutableSet.copyOf(expansions);
  }
}
