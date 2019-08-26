package com.epgpbot.epgpbot.schema.game;

import com.google.common.collect.ImmutableSet;

final class RaceAssignment {
  public final CharacterRace race;
  public final ImmutableSet<Expansion> expansions;

  public RaceAssignment(CharacterRace race, Expansion[] expansions) {
    this.race = race;
    this.expansions = ImmutableSet.copyOf(expansions);
  }

  public RaceAssignment(CharacterRace race) {
    this.race = race;
    this.expansions = ImmutableSet.of();
  }
}

public enum CharacterClass {
  PALADIN(
      "Paladin",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.DWARF),
          new RaceAssignment(CharacterRace.HUMAN),
          new RaceAssignment(CharacterRace.DRAENEI),
          new RaceAssignment(CharacterRace.BLOOD_ELF),
          new RaceAssignment(CharacterRace.TAUREN, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.LIGHTFORGED_DRAENEI),
          new RaceAssignment(CharacterRace.DARK_IRON_DWARF),
          new RaceAssignment(CharacterRace.ZANDALARI),
      },
      Expansion.since(Expansion.VANILLA)
  ),
  MAGE(
      "Mage",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.DRAENEI),
          new RaceAssignment(CharacterRace.DWARF, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.GNOME),
          new RaceAssignment(CharacterRace.HUMAN),
          new RaceAssignment(CharacterRace.NIGHT_ELF, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.WORGEN),
          new RaceAssignment(CharacterRace.VOID_ELF),
          new RaceAssignment(CharacterRace.LIGHTFORGED_DRAENEI),
          new RaceAssignment(CharacterRace.DARK_IRON_DWARF),
          new RaceAssignment(CharacterRace.KUL_TIRAN_HUMAN),
          new RaceAssignment(CharacterRace.PANDAREN),
          new RaceAssignment(CharacterRace.BLOOD_ELF),
          new RaceAssignment(CharacterRace.GOBLIN),
          new RaceAssignment(CharacterRace.ORC, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.TROLL),
          new RaceAssignment(CharacterRace.UNDEAD),
          new RaceAssignment(CharacterRace.NIGHTBORNE),
          new RaceAssignment(CharacterRace.MAGHAR),
          new RaceAssignment(CharacterRace.ZANDALARI),
      },
      Expansion.since(Expansion.VANILLA)
  ),
  DRUID(
      "Druid",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.NIGHT_ELF),
          new RaceAssignment(CharacterRace.WORGEN),
          new RaceAssignment(CharacterRace.KUL_TIRAN_HUMAN),
          new RaceAssignment(CharacterRace.TAUREN),
          new RaceAssignment(CharacterRace.TROLL, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.HIGHMOUNTAIN_TAUREN),
          new RaceAssignment(CharacterRace.ZANDALARI),
      },
      Expansion.since(Expansion.VANILLA)
  ),
  HUNTER(
      "Hunter",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.DRAENEI),
          new RaceAssignment(CharacterRace.DWARF),
          new RaceAssignment(CharacterRace.GNOME, Expansion.since(Expansion.LEGION)),
          new RaceAssignment(CharacterRace.HUMAN, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.NIGHT_ELF),
          new RaceAssignment(CharacterRace.WORGEN),
          new RaceAssignment(CharacterRace.VOID_ELF),
          new RaceAssignment(CharacterRace.LIGHTFORGED_DRAENEI),
          new RaceAssignment(CharacterRace.DARK_IRON_DWARF),
          new RaceAssignment(CharacterRace.KUL_TIRAN_HUMAN),
          new RaceAssignment(CharacterRace.PANDAREN),
          new RaceAssignment(CharacterRace.BLOOD_ELF),
          new RaceAssignment(CharacterRace.GOBLIN),
          new RaceAssignment(CharacterRace.ORC),
          new RaceAssignment(CharacterRace.TAUREN),
          new RaceAssignment(CharacterRace.TROLL),
          new RaceAssignment(CharacterRace.UNDEAD, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.NIGHTBORNE),
          new RaceAssignment(CharacterRace.HIGHMOUNTAIN_TAUREN),
          new RaceAssignment(CharacterRace.MAGHAR),
          new RaceAssignment(CharacterRace.ZANDALARI),
      },
      Expansion.since(Expansion.VANILLA)
  ),
  PRIEST(
      "Priest",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.DRAENEI),
          new RaceAssignment(CharacterRace.DWARF),
          new RaceAssignment(CharacterRace.GNOME, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.HUMAN),
          new RaceAssignment(CharacterRace.NIGHT_ELF),
          new RaceAssignment(CharacterRace.WORGEN),
          new RaceAssignment(CharacterRace.VOID_ELF),
          new RaceAssignment(CharacterRace.LIGHTFORGED_DRAENEI),
          new RaceAssignment(CharacterRace.DARK_IRON_DWARF),
          new RaceAssignment(CharacterRace.KUL_TIRAN_HUMAN),
          new RaceAssignment(CharacterRace.PANDAREN),
          new RaceAssignment(CharacterRace.BLOOD_ELF),
          new RaceAssignment(CharacterRace.GOBLIN),
          new RaceAssignment(CharacterRace.TROLL),
          new RaceAssignment(CharacterRace.TAUREN, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.UNDEAD),
          new RaceAssignment(CharacterRace.NIGHTBORNE),
          new RaceAssignment(CharacterRace.MAGHAR),
          new RaceAssignment(CharacterRace.ZANDALARI),
      },
      Expansion.since(Expansion.VANILLA)
  ),
  ROGUE(
      "Rogue",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.DWARF),
          new RaceAssignment(CharacterRace.GNOME),
          new RaceAssignment(CharacterRace.HUMAN),
          new RaceAssignment(CharacterRace.NIGHT_ELF),
          new RaceAssignment(CharacterRace.WORGEN),
          new RaceAssignment(CharacterRace.VOID_ELF),
          new RaceAssignment(CharacterRace.DARK_IRON_DWARF),
          new RaceAssignment(CharacterRace.KUL_TIRAN_HUMAN),
          new RaceAssignment(CharacterRace.PANDAREN),
          new RaceAssignment(CharacterRace.BLOOD_ELF),
          new RaceAssignment(CharacterRace.GOBLIN),
          new RaceAssignment(CharacterRace.ORC),
          new RaceAssignment(CharacterRace.TROLL),
          new RaceAssignment(CharacterRace.UNDEAD),
          new RaceAssignment(CharacterRace.NIGHTBORNE),
          new RaceAssignment(CharacterRace.MAGHAR),
          new RaceAssignment(CharacterRace.ZANDALARI),
      },
      Expansion.since(Expansion.VANILLA)
  ),
  SHAMAN(
      "Shaman",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.DRAENEI),
          new RaceAssignment(CharacterRace.DWARF, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.DARK_IRON_DWARF),
          new RaceAssignment(CharacterRace.KUL_TIRAN_HUMAN),
          new RaceAssignment(CharacterRace.PANDAREN),
          new RaceAssignment(CharacterRace.GOBLIN),
          new RaceAssignment(CharacterRace.ORC),
          new RaceAssignment(CharacterRace.TAUREN),
          new RaceAssignment(CharacterRace.TROLL),
          new RaceAssignment(CharacterRace.HIGHMOUNTAIN_TAUREN),
          new RaceAssignment(CharacterRace.MAGHAR),
          new RaceAssignment(CharacterRace.ZANDALARI),
      },
      Expansion.since(Expansion.VANILLA)
  ),
  WARLOCK(
      "Warlock",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.DWARF, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.GNOME),
          new RaceAssignment(CharacterRace.HUMAN),
          new RaceAssignment(CharacterRace.WORGEN),
          new RaceAssignment(CharacterRace.VOID_ELF),
          new RaceAssignment(CharacterRace.DARK_IRON_DWARF),
          new RaceAssignment(CharacterRace.BLOOD_ELF),
          new RaceAssignment(CharacterRace.GOBLIN),
          new RaceAssignment(CharacterRace.ORC),
          new RaceAssignment(CharacterRace.TROLL, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.UNDEAD),
          new RaceAssignment(CharacterRace.NIGHTBORNE),
      },
      Expansion.since(Expansion.VANILLA)
  ),
  WARRIOR(
      "Warrior",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.DRAENEI),
          new RaceAssignment(CharacterRace.DWARF),
          new RaceAssignment(CharacterRace.GNOME),
          new RaceAssignment(CharacterRace.HUMAN),
          new RaceAssignment(CharacterRace.NIGHT_ELF),
          new RaceAssignment(CharacterRace.WORGEN),
          new RaceAssignment(CharacterRace.VOID_ELF),
          new RaceAssignment(CharacterRace.LIGHTFORGED_DRAENEI),
          new RaceAssignment(CharacterRace.DARK_IRON_DWARF),
          new RaceAssignment(CharacterRace.KUL_TIRAN_HUMAN),
          new RaceAssignment(CharacterRace.PANDAREN),
          new RaceAssignment(CharacterRace.BLOOD_ELF, Expansion.since(Expansion.CATACLYSM)),
          new RaceAssignment(CharacterRace.GOBLIN),
          new RaceAssignment(CharacterRace.ORC),
          new RaceAssignment(CharacterRace.TAUREN),
          new RaceAssignment(CharacterRace.TROLL),
          new RaceAssignment(CharacterRace.UNDEAD),
          new RaceAssignment(CharacterRace.NIGHTBORNE),
          new RaceAssignment(CharacterRace.HIGHMOUNTAIN_TAUREN),
          new RaceAssignment(CharacterRace.MAGHAR),
          new RaceAssignment(CharacterRace.ZANDALARI),
      },
      Expansion.since(Expansion.VANILLA)
  ),
  DEATH_KNIGHT(
      "Death Knight",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.DRAENEI),
          new RaceAssignment(CharacterRace.DWARF),
          new RaceAssignment(CharacterRace.GNOME),
          new RaceAssignment(CharacterRace.HUMAN),
          new RaceAssignment(CharacterRace.NIGHT_ELF),
          new RaceAssignment(CharacterRace.WORGEN),
          new RaceAssignment(CharacterRace.BLOOD_ELF),
          new RaceAssignment(CharacterRace.GOBLIN),
          new RaceAssignment(CharacterRace.ORC),
          new RaceAssignment(CharacterRace.TAUREN),
          new RaceAssignment(CharacterRace.TROLL),
          new RaceAssignment(CharacterRace.UNDEAD),
      },
      Expansion.since(Expansion.WRATH_OF_THE_LICH_KING)
  ),
  MONK(
      "Monk",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.DRAENEI, Expansion.since(Expansion.MISTS_OF_PANDARIA)),
          new RaceAssignment(CharacterRace.DWARF, Expansion.since(Expansion.MISTS_OF_PANDARIA)),
          new RaceAssignment(CharacterRace.GNOME, Expansion.since(Expansion.MISTS_OF_PANDARIA)),
          new RaceAssignment(CharacterRace.HUMAN, Expansion.since(Expansion.MISTS_OF_PANDARIA)),
          new RaceAssignment(CharacterRace.NIGHT_ELF, Expansion.since(Expansion.MISTS_OF_PANDARIA)),
          new RaceAssignment(CharacterRace.VOID_ELF),
          new RaceAssignment(CharacterRace.DARK_IRON_DWARF),
          new RaceAssignment(CharacterRace.KUL_TIRAN_HUMAN),
          new RaceAssignment(CharacterRace.PANDAREN),
          new RaceAssignment(CharacterRace.BLOOD_ELF, Expansion.since(Expansion.MISTS_OF_PANDARIA)),
          new RaceAssignment(CharacterRace.ORC, Expansion.since(Expansion.MISTS_OF_PANDARIA)),
          new RaceAssignment(CharacterRace.TAUREN, Expansion.since(Expansion.MISTS_OF_PANDARIA)),
          new RaceAssignment(CharacterRace.TROLL, Expansion.since(Expansion.MISTS_OF_PANDARIA)),
          new RaceAssignment(CharacterRace.UNDEAD, Expansion.since(Expansion.MISTS_OF_PANDARIA)),
          new RaceAssignment(CharacterRace.NIGHTBORNE),
          new RaceAssignment(CharacterRace.HIGHMOUNTAIN_TAUREN),
          new RaceAssignment(CharacterRace.MAGHAR),
          new RaceAssignment(CharacterRace.ZANDALARI),
      },
      Expansion.since(Expansion.MISTS_OF_PANDARIA)
  ),
  DEMON_HUNTER(
      "Demon Hunter",
      new RaceAssignment[]{
          new RaceAssignment(CharacterRace.NIGHT_ELF),
          new RaceAssignment(CharacterRace.BLOOD_ELF),
      },
      Expansion.since(Expansion.LEGION)
  );

  public final String name;
  public final ImmutableSet<RaceAssignment> races;
  public final ImmutableSet<Expansion> expansions;

  private CharacterClass(String name, RaceAssignment[] races, Expansion[] expansions) {
    this.name = name;
    this.races = ImmutableSet.copyOf(races);
    this.expansions = ImmutableSet.copyOf(expansions);
  }
}
