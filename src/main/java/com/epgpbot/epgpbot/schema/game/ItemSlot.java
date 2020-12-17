package com.epgpbot.epgpbot.schema.game;

public enum ItemSlot {
  NONE,
  HEAD,
  NECK,
  SHOULDERS,
  SHIRT,
  CHEST,
  WAIST,
  LEGS,
  FEET,
  WRISTS,
  HANDS,
  FINGER,
  TRINKET,
  WEAPON_1H, // treat as WEAPON
  SHIELD,
  RANGED, // treat as RANGED (bow)
  CLOAK,
  WEAPON_2H, // treat as WEAPON
  BAG,
  TABARD,
  ROBE,  // treat as CHEST (includes robes, probably for display reasons)
  WEAPON_MH, // treat as WEAPON
  WEAPON_OH, // treat as WEAPON
  HELD_IN_OH,
  AMMO,
  THROWN, // treat as RANGED
  RANGED2, // treat as RANGED (crossbow, gun, wand)
  QUIVER,
  RELIC; // treat as RANGED
}
