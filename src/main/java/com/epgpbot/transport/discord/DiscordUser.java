package com.epgpbot.transport.discord;

import java.util.HashSet;
import java.util.Set;

import com.epgpbot.config.Config;
import com.epgpbot.database.Cursor;
import com.epgpbot.database.Database;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

public class DiscordUser implements com.epgpbot.transport.User {
  private long playerId;
  private String playerName;
  private final User user;
  private final Member member;
  private final Config config;

  public DiscordUser(Database db, Config config, User user, Member member) throws Exception {
    this.config = config;
    this.user = user;
    this.member = member;
    this.playerId = -1;
    this.playerName = null;

    // Find the linked player record (if any).
    // Must be explicitly linked by an admin - players cannot link themselves.
    try (Transaction tx = db.transaction()) {
      try (Statement q = tx.prepare(
            "SELECT p.id, p.name "
          + "FROM players AS p "
          + "JOIN transport_users AS tu ON tu.player_id = p.id "
          + "WHERE tu.id = :transport_id;")) {
        q.bind("transport_id", this.transportUserId());
        try (Cursor r = q.executeFetch()) {
          if (r.next()) {
            this.playerId = r.get("id", Long.class);
            this.playerName = r.get("name", String.class);
          }
        }
      }
    }
  }

  @Override
  public long playerId() {
    return playerId;
  }

  @Override
  public String playerName() {
    return playerName;
  }

  @Override
  public boolean hasPermission(PermissionType permission) {
    return permissions().contains(permission);
  }

  @Override
  public boolean hasPermissions(Iterable<PermissionType> permissions) {
    for (PermissionType p : permissions) {
      if (!hasPermission(p)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String transportUserId() {
    return user.getId();
  }

  @Override
  public String transportUserName() {
    if (member != null) {
      // Use the nickname for the guild server (if any).
      return member.getEffectiveName();
    }

    for (Guild g : user.getMutualGuilds()) {
      if (g.getId().equals(config.discord_server_id)) {
        Member m = g.getMember(user);
        return m.getEffectiveName();
      }
    }

    // Default to discord account name.
    return user.getName();
  }

  @Override
  public boolean hasPlayer() {
    return playerId >= 0;
  }

  @Override
  public Set<PermissionType> permissions() {
    Set<PermissionType> permissions = new HashSet<>();

    if (member != null) {
      // If we're in a server chat room:
      if (member.getGuild().getId().equals(config.discord_server_id)) {
        // Only pay attention to the correct server.
        for (Role r : member.getRoles()) {
          // Gain permissions based on their role in the server.
          Set<PermissionType> rolePermissions = config.discord_ranks_to_permissions.get(r.getName().toLowerCase());
          if (rolePermissions != null) {
            permissions.addAll(rolePermissions);
          }
        }
      }
    }
    else {
      // If we're in a private message channel:
      for (Guild g : user.getMutualGuilds()) {
        // Check that the user is a member of the correct server.
        if (!g.getId().equals(config.discord_server_id)) {
          continue;
        }

        // If they are, get their roles on the server.
        Member m = g.getMember(user);
        for (Role r : m.getRoles()) {
          // Gain permissions based on their role in the server.
          Set<PermissionType> rolePermissions = config.discord_ranks_to_permissions.get(r.getName().toLowerCase());
          if (rolePermissions != null) {
            permissions.addAll(rolePermissions);
          }
        }
      }
    }

    Set<PermissionType> uidPermissions = config.discord_users_to_permissions.get(user.getId());
    if (uidPermissions != null) {
      permissions.addAll(uidPermissions);
    }

    return permissions;
  }
}
