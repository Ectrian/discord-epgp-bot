package com.epgpbot.config;

import java.util.Map;
import java.util.Set;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.epgpbot.schema.game.Expansion;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class Config {
  // Name of your in-game guild.
  public String game_guild_name;

  // Name of the in-game realm on which you play.
  public String game_realm_name;

  // Base URL of the armory (e.g. armory.server.com).
  // Leave NULL if armory disabled.
  public String game_armory_url = null;

  // What type of connection to use for the armory API.
  // Note: Classic WoW does not currently support an armory API. Use NONE.
  public ArmoryType game_armory_type = ArmoryType.NONE;

  // Expansion you are targeting.
  // Note: Only Classic WoW is officially supported. Use other expansions at your own risk.
  public Expansion game_expansion = Expansion.VANILLA;

  // Authentication token for Discord Bot User.
  // Refer to readme.md for instructions on creating a token.
  public String discord_bot_token;

  // Discord server ID for which bot will respond and base permissions on.
  // Obtainable from Server Settings > Widget > Server ID.
  public String discord_server_id;

  // Channel names (not prefixed by #) on your server in which the bot will listen.
  // Bot will always respond to PMs.
  // Case-sensitive.
  public Set<String> discord_channels = ImmutableSet.of();

  // Mapping of Discord Rank Names (on your server) to permissions available.
  // USE ALL LOWER-CASE ROLE NAMES IN YOUR CONFIGURATION!
  // Generally:
  // - Guild master has all permissions
  // - Officers have VIEW_EPGP, MODIFY_EPGP
  // - Members have VIEW_EPGP
  public Map<String, Set<PermissionType>> discord_ranks_to_permissions = ImmutableMap.of();

  // Mapping of Discord User IDs to permissions available.
  // To obtain an ID, enable developer mode in discord settings. You can now copy user IDs.
  public Map<String, Set<PermissionType>> discord_users_to_permissions = ImmutableMap.of();

  // List of officer player names (used for !guild.officers). Case-sensitive.
  // For Classic WoW, leave empty.
  public Set<String> officer_player_names = ImmutableSet.of();

  // EPGP Decay Rate (e.g. 10 = 10% decay).
  public long epgp_decay_rate = 10;

  // EPGP Base GP (e.g. 500).
  public long epgp_base_gp = 600;

  // EPGP Decay Base GP (e.g. 500) - must be <= BASE_GP.
  // Sets the amount of BASE_GP that is considered for decay.
  // NOTE: In the standard EPGP configuration, DECAY_BASE_GP = BASE_GP.
  public long epgp_decay_base_gp = 600;

  // Connection information for MySQL 8+ server.
  public String database_host = "localhost";
  public int database_port = 3306;
  public String database_user = "root";
  public String database_password = "root";
  public String database_name = "epgpdb";
  public boolean database_use_tls = false;

  // What type of loot system to use.
  // Note: Only EPGP is currently supported.
  public LootMethod loot_method = LootMethod.EPGP;
}
