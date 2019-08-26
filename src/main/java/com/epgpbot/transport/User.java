package com.epgpbot.transport;

import java.util.Set;

import com.epgpbot.epgpbot.schema.PermissionType;

public interface User {
  public String transportUserId();
  public String transportUserName();
  public boolean hasPlayer();
  public long playerId();
  public String playerName();
  public boolean hasPermission(PermissionType permission);
  public boolean hasPermissions(Iterable<PermissionType> permissions);
  public Set<PermissionType> permissions();
}
