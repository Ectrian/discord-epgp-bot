package com.epgpbot.epgpbot.schema.game;

import java.util.Arrays;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

public enum ItemSlotGroup {
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
  FINGER(ImmutableSet.of(ItemSlot.FINGER)),
  TRINKET(ImmutableSet.of(ItemSlot.TRINKET)),
  SHIELD(ImmutableSet.of(ItemSlot.SHIELD)),
  RANGED(ImmutableSet.of(ItemSlot.RANGED, ItemSlot.THROWN, ItemSlot.RANGED2, ItemSlot.RELIC)),
  WEAPON(ImmutableSet.of(ItemSlot.WEAPON_1H, ItemSlot.WEAPON_2H, ItemSlot.WEAPON_MH, ItemSlot.WEAPON_OH, ItemSlot.HELD_IN_OH)),
  CLOAK(ImmutableSet.of(ItemSlot.CLOAK)),
  TABARD(ImmutableSet.of(ItemSlot.TABARD));

  public final ImmutableSet<ItemSlot> slots;

  private ItemSlotGroup(ImmutableSet<ItemSlot> slots) {
    this.slots = slots;
  }

  public static ItemSlotGroup forString(String s) {
    for (ItemSlotGroup e : values()) {
      if (s.toLowerCase().equals(e.toString().toLowerCase())) {
        return e;
      }
    }
    return null;
  }

  public static String docChoices() {
    return Joiner.on("|").join(
        Arrays
          .stream(values())
          .map(v -> v.toString().toLowerCase())
          .iterator());
  }
}
