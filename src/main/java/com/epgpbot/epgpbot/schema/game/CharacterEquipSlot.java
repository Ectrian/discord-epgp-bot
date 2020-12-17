package com.epgpbot.epgpbot.schema.game;

import com.google.common.collect.ImmutableSet;

public enum CharacterEquipSlot {
  HEAD(ImmutableSet.of(ItemSlot.HEAD)),
  NECK(ImmutableSet.of(ItemSlot.NECK)),
  SHOULDERS(ImmutableSet.of(ItemSlot.SHOULDERS)),
  SHIRT(ImmutableSet.of(ItemSlot.SHIRT)),
  CHEST(ImmutableSet.of(ItemSlot.CHEST, ItemSlot.ROBE)),
  WAIST(ImmutableSet.of(ItemSlot.WAIST)),
  LEGS(ImmutableSet.of(ItemSlot.LEGS)),
  FEET(ImmutableSet.of(ItemSlot.FEET)),
  WRISTS(ImmutableSet.of(ItemSlot.WRISTS)),
  HANDS(ImmutableSet.of(ItemSlot.HANDS)),
  FINGER1(ImmutableSet.of(ItemSlot.FINGER)),
  FINGER2(ImmutableSet.of(ItemSlot.FINGER)),
  TRINKET1(ImmutableSet.of(ItemSlot.TRINKET)),
  TRINKET2(ImmutableSet.of(ItemSlot.TRINKET)),
  MAINHAND(ImmutableSet.of(ItemSlot.WEAPON_2H, ItemSlot.WEAPON_MH, ItemSlot.WEAPON_1H)),
  OFFHAND(ImmutableSet.of(ItemSlot.WEAPON_1H, ItemSlot.WEAPON_OH, ItemSlot.HELD_IN_OH, ItemSlot.SHIELD)),
  RANGED(ImmutableSet.of(ItemSlot.RANGED, ItemSlot.RANGED2, ItemSlot.RELIC, ItemSlot.THROWN)),
  AMMO(ImmutableSet.of(ItemSlot.AMMO)),
  CLOAK(ImmutableSet.of(ItemSlot.CLOAK)),
  TABARD(ImmutableSet.of(ItemSlot.TABARD));

  public final ImmutableSet<ItemSlot> accepts;

  private CharacterEquipSlot(ImmutableSet<ItemSlot> accepts) {
    this.accepts = accepts;
  }
}
