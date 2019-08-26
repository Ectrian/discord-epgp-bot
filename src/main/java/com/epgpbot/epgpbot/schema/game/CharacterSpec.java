package com.epgpbot.epgpbot.schema.game;

import com.google.common.collect.ImmutableSet;

public enum CharacterSpec {
  DEMON_HUNTER_HAVOC(
      "Havoc",
      CharacterClass.DEMON_HUNTER,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  DEMON_HUNTER_VENGEANCE(
      "Vengeance",
      CharacterClass.DEMON_HUNTER,
      new CharacterRole[] {CharacterRole.TANK}
  ),
  MONK_BREWMASTER(
      "Brewmaster",
      CharacterClass.MONK,
      new CharacterRole[] {CharacterRole.TANK}
  ),
  MONK_MISTWEAVER(
      "Mistweaver",
      CharacterClass.MONK,
      new CharacterRole[] {CharacterRole.HEALER}
  ),
  MONK_WINDWALKER(
      "Windwalker",
      CharacterClass.MONK,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  DEATH_KNIGHT_BLOOD(
      "Blood",
      CharacterClass.DEATH_KNIGHT,
      new CharacterRole[] {CharacterRole.TANK}
  ),
  DEATH_KNIGHT_FROST(
      "Frost",
      CharacterClass.DEATH_KNIGHT,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  DEATH_KNIGHT_UNHOLY(
      "Unholy",
      CharacterClass.DEATH_KNIGHT,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  WARRIOR_ARMS(
      "Arms",
      CharacterClass.WARRIOR,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  WARRIOR_FURY(
      "Fury",
      CharacterClass.WARRIOR,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  WARRIOR_PROTECTION(
      "Protection",
      CharacterClass.WARRIOR,
      new CharacterRole[] {CharacterRole.TANK}
  ),
  WARLOCK_AFFLICTION(
      "Affliction",
      CharacterClass.WARLOCK,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  WARLOCK_DEMONOLOGY(
      "Demonology",
      CharacterClass.WARLOCK,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  WARLOCK_DESTRUCTION(
      "Destruction",
      CharacterClass.WARLOCK,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  SHAMAN_ENHANCEMENT(
      "Enhancement",
      CharacterClass.SHAMAN,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  SHAMAN_ELEMENTAL(
      "Elemental",
      CharacterClass.SHAMAN,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  SHAMAN_RESTORATION(
      "Restoration",
      CharacterClass.SHAMAN,
      new CharacterRole[] {CharacterRole.HEALER}
  ),
  ROGUE_SUBTLETY(
      "Subtlety",
      CharacterClass.ROGUE,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  ROGUE_COMBAT(
      "Combat",
      CharacterClass.ROGUE,
      new CharacterRole[] {CharacterRole.DPS},
      Expansion.until(Expansion.WARLORDS_OF_DRAENOR)
  ),
  ROGUE_OUTLAW(
      "Outlaw",
      CharacterClass.ROGUE,
      new CharacterRole[] {CharacterRole.DPS},
      Expansion.since(Expansion.WARLORDS_OF_DRAENOR)
  ),
  ROGUE_ASSASSINATION(
      "Assassination",
      CharacterClass.ROGUE,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  PRIEST_SHADOW(
      "Shadow",
      CharacterClass.PRIEST,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  PRIEST_DISCIPLINE(
      "Discipline",
      CharacterClass.PRIEST,
      new CharacterRole[] {CharacterRole.HEALER}
  ),
  PRIEST_HOLY(
      "Holy",
      CharacterClass.PRIEST,
      new CharacterRole[] {CharacterRole.HEALER}
  ),
  PALADIN_HOLY(
      "Holy",
      CharacterClass.PALADIN,
      new CharacterRole[] {CharacterRole.HEALER}
  ),
  PALADIN_PROTECTION(
      "Protection",
      CharacterClass.PALADIN,
      new CharacterRole[] {CharacterRole.TANK}
  ),
  PALADIN_RETRIBUTION(
      "Retribution",
      CharacterClass.PALADIN,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  MAGE_ARCANE(
      "Arcane",
      CharacterClass.MAGE,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  MAGE_FROST(
      "Frost",
      CharacterClass.MAGE,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  MAGE_FIRE(
      "Fire",
      CharacterClass.MAGE,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  HUNTER_MARKSMANSHIP(
      "Marksmanship",
      CharacterClass.HUNTER,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  HUNTER_SURVIVAL(
      "Survival",
      CharacterClass.HUNTER,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  HUNTER_BEAST_MASTERY(
      "Beast Mastery",
      CharacterClass.HUNTER,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  DRUID_RESTORATION(
      "Restoration",
      CharacterClass.DRUID,
      new CharacterRole[] {CharacterRole.HEALER}
  ),
  DRUID_FERAL(
      "Feral",
      CharacterClass.DRUID,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  DRUID_BALANCE(
      "Balance",
      CharacterClass.DRUID,
      new CharacterRole[] {CharacterRole.DPS}
  ),
  DRUID_GUARDIAN(
      "Guardian",
      CharacterClass.DRUID,
      new CharacterRole[] {CharacterRole.TANK},
      Expansion.since(Expansion.MISTS_OF_PANDARIA)
  ),
  ;

  public final String name;
  public final CharacterClass clazz;
  public final ImmutableSet<CharacterRole> roles;
  public final ImmutableSet<Expansion> expansions;

  private CharacterSpec(String name, CharacterClass clazz, CharacterRole[] roles) {
    this.name = name;
    this.clazz = clazz;
    this.roles = ImmutableSet.copyOf(roles);
    this.expansions = ImmutableSet.copyOf(Expansion.values());
  }

  private CharacterSpec(String name, CharacterClass clazz, CharacterRole[] roles, Expansion[] expansions) {
    this.name = name;
    this.clazz = clazz;
    this.roles = ImmutableSet.copyOf(roles);
    this.expansions = ImmutableSet.copyOf(expansions);
  }
}
