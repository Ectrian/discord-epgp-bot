package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.PlayerId;
import com.epgpbot.util.TablePageSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class CharacterWhoCanPlayCommandHandler extends CommandHandlerAbstract {
  static class ClassAndSpec {
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
      result = prime * result + ((spec == null) ? 0 : spec.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ClassAndSpec other = (ClassAndSpec) obj;
      if (clazz == null) {
        if (other.clazz != null)
          return false;
      } else if (!clazz.equals(other.clazz))
        return false;
      if (spec == null) {
        if (other.spec != null)
          return false;
      } else if (!spec.equals(other.spec))
        return false;
      return true;
    }

    public final String clazz;
    public final String spec;

    private ClassAndSpec(String clazz, String spec) {
      this.clazz = clazz.toLowerCase();
      this.spec = spec.toLowerCase();
    }

    public static ClassAndSpec fromOptionals(Optional<String> clazz, Optional<String> spec) {
      if (!clazz.isPresent()) {
        return null;
      }
      if (!spec.isPresent()) {
        return null;
      }
      return new ClassAndSpec(clazz.get(), spec.get());
    }
  }

  private static final Map<String, Set<ClassAndSpec>> ROLES =
      new ImmutableMap.Builder<String, Set<ClassAndSpec>>().put("tank",
          new ImmutableSet.Builder<ClassAndSpec>().add(new ClassAndSpec("warrior", "protection"))
              .add(new ClassAndSpec("paladin", "protection"))
              .add(new ClassAndSpec("death knight", "blood")) // Not perfectly accurate.
              .add(new ClassAndSpec("druid", "feral combat")) // Not perfectly accurate.
              .build())
          .put("healer",
              new ImmutableSet.Builder<ClassAndSpec>().add(new ClassAndSpec("druid", "restoration"))
                  .add(new ClassAndSpec("paladin", "holy")).add(new ClassAndSpec("priest", "holy"))
                  .add(new ClassAndSpec("priest", "discipline"))
                  .add(new ClassAndSpec("shaman", "restoration")).build())
          .put("dps",
              new ImmutableSet.Builder<ClassAndSpec>().add(new ClassAndSpec("hunter", "marksman"))
                  .add(new ClassAndSpec("hunter", "survival"))
                  .add(new ClassAndSpec("hunter", "beast Mastery"))
                  .add(new ClassAndSpec("warlock", "affliction"))
                  .add(new ClassAndSpec("warlock", "demonology"))
                  .add(new ClassAndSpec("warlock", "destruction"))
                  .add(new ClassAndSpec("warrior", "arms")).add(new ClassAndSpec("warrior", "fury"))
                  .add(new ClassAndSpec("death knight", "unholy")) // Not perfectly accurate.
                  .add(new ClassAndSpec("death knight", "frost")) // Not perfectly accurate.
                  .add(new ClassAndSpec("death knight", "blood")) // Not perfectly accurate.
                  .add(new ClassAndSpec("druid", "balance"))
                  .add(new ClassAndSpec("druid", "feral Combat")) // Not perfectly accurate.
                  .add(new ClassAndSpec("paladin", "retribution"))
                  .add(new ClassAndSpec("priest", "shadow"))
                  .add(new ClassAndSpec("shaman", "elemental"))
                  .add(new ClassAndSpec("shaman", "enhancement"))
                  .add(new ClassAndSpec("mage", "arcane")).add(new ClassAndSpec("mage", "fire"))
                  .add(new ClassAndSpec("mage", "frost")).add(new ClassAndSpec("rogue", "combat"))
                  .add(new ClassAndSpec("rogue", "assassination"))
                  .add(new ClassAndSpec("rogue", "subtlety")).build())
          .build();

  private static Set<String> flagSet(Request request, String name) {
    Set<String> out = new HashSet<>();
    for (String s : request.flag(name)) {
      out.add(s.toLowerCase());
    }
    return out;
  }

  // TODO: Standardize display of character list.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.flags().isEmpty()) {
      sendCorrectUsage(context);
      return;
    }

    List<Map<String, Object>> table = new ArrayList<>();
    Set<Long> filterToPlayers = new HashSet<>();
    Set<String> filterToClasses = flagSet(request, "class");
    Set<String> filterToSpecs = flagSet(request, "spec");
    Set<ClassAndSpec> filterToRoles = new HashSet<>();

    for (String role : request.flag("role")) {
      if (!ROLES.containsKey(role.toLowerCase())) {
        sendError(context, "Unknown role '%s'.", role);
        return;
      }
      filterToRoles.addAll(ROLES.get(role.toLowerCase()));
    }

    try (Transaction tx = context.database().transaction()) {
      for (String characterName : request.flag("alts-of")) {
        PlayerId playerId = getPlayerIdForCharacter(tx, characterName);
        if (playerId == null) {
          sendError(context, "Unknown or unlinked character '%s'", ucfirst(characterName));
          return;
        }
        filterToPlayers.add(playerId.id);
      }

      try (Statement q = tx.prepare("SELECT c.*, p.name AS player_name " + "FROM characters AS c "
          + "JOIN players AS p ON p.id = c.player_id " + "WHERE " + "  c.player_id IS NOT NULL "
          + "  AND c.deleted = 0 " + ";")) {
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            long playerId = r.get("player_id", Long.class);
            Optional<String> spec1 = r.getNullable("talent_spec1_tree", String.class);
            Optional<String> spec2 = r.getNullable("talent_spec2_tree", String.class);
            Optional<String> clazz = r.getNullable("class", String.class);

            if (!filterToPlayers.isEmpty() && !filterToPlayers.contains(playerId)) {
              continue;
            }

            if (!filterToClasses.isEmpty()) {
              if (!clazz.isPresent()) {
                continue;
              }
              if (!filterToClasses.contains(clazz.get().toLowerCase())) {
                continue;
              }
            }

            if (!filterToSpecs.isEmpty()) {
              boolean matchSpec1 =
                  spec1.isPresent() && filterToSpecs.contains(spec1.get().toLowerCase());
              boolean matchSpec2 =
                  spec2.isPresent() && filterToSpecs.contains(spec2.get().toLowerCase());
              if (!matchSpec1 && !matchSpec2) {
                continue;
              }
            }

            if (!filterToRoles.isEmpty()) {
              ClassAndSpec cspec1 = ClassAndSpec.fromOptionals(clazz, spec1);
              ClassAndSpec cspec2 = ClassAndSpec.fromOptionals(clazz, spec2);
              boolean matchSpec1 = cspec1 != null && filterToRoles.contains(cspec1);
              boolean matchSpec2 = cspec2 != null && filterToRoles.contains(cspec2);
              if (!matchSpec1 && !matchSpec2) {
                continue;
              }
            }

            Map<String, Object> row = new HashMap<>();
            row.put("player", r.get("player_name", String.class));
            row.put("name", r.get("name", String.class));
            row.put("level", r.getNullable("level", Long.class));
            row.put("race", r.getNullable("race", String.class));
            row.put("class", clazz);
            row.put("guild", r.getNullable("guild_name", String.class));
            row.put("spec1", spec1);
            row.put("spec2", spec2);
            table.add(row);
          }
        }
      }
    }

    context.replyWithPages(new TablePageSource("Search Results", table,
        ImmutableList.of("player", "name", "level", "race", "class", "guild", "spec1", "spec2"),
        ImmutableSet.of()));
  }

  @Override
  public String help() {
    return "[--class <...:string>] [--spec <...:string>] [--role <'healer'|'tank'|'dps'>] [--alts-of <...character:string>] - Displays who is able to play a particular class, spec, or role.";
  }

  @Override
  public String command() {
    return "character.whocanplay";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
