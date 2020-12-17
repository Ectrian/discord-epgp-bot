package com.epgpbot.epgpbot.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.epgpbot.database.ArrayParameter;
import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.epgpbot.schema.RaidType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.Argument;
import com.epgpbot.util.TablePageSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class AttendanceCommandHandler extends CommandHandlerAbstract {
  public static final ArrayParameter<EPGPEventType> P_TYPES = ArrayParameter.declare("event_types", EPGPEventType.class);
  public static final ArrayParameter<RaidType> P_RAIDS = ArrayParameter.declare("raid_types", RaidType.class);
  public static final Set<EPGPEventType> ATTENDANCE_TYPES = ImmutableSet.of(EPGPEventType.RAID, EPGPEventType.STANDBY);
  public static final String ATTENDANCE_QUERY_BASE =
      "SELECT " +
      "  p.id AS player_id, p.name AS player, COUNT(*) as raids " +
      "FROM " +
      "  players AS p " +
      "LEFT JOIN " +
      "  epgp_log AS l ON l.target_player_id = p.id " +
      "WHERE " +
      "    l.type IN " + Statement.in(P_TYPES, ATTENDANCE_TYPES) + " AND " +
      "    l.undoes IS NULL AND " +
      "    l.undone_by IS NULL %s" +
      "GROUP BY " +
      "  p.id " +
      "ORDER BY raids DESC;";

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> table;
    Set<Long> include = new HashSet<>();
    Set<RaidType> raids = new HashSet<>();

    for (Argument a : request.flagArgs("raid")) {
      raids.add(a.enumValue(RaidType.class));
    }

    try (Transaction tx = context.database().transaction()) {
      String query = String.format(ATTENDANCE_QUERY_BASE, "");
      if (!raids.isEmpty()) {
        query = String.format(ATTENDANCE_QUERY_BASE, "AND l.raid_type IN " + Statement.in(P_RAIDS, raids));
      }

      try (Statement q = tx.prepare(query)) {
        q.bind(P_TYPES, ATTENDANCE_TYPES);
        if (!raids.isEmpty()) {
          q.bind(P_RAIDS, raids);
        }
        try (Cursor r = q.executeFetch()) {
          table = r.toList();
        }
      }

      for (Argument a : request.args("character")) {
          include.add(a.characterPlayerIdValue(tx).id);
      }
    }

    if (!include.isEmpty()) {
      table = table.stream()
          .filter(row -> include.contains(row.get("player_id")))
          .collect(Collectors.toList());
    }

    context.replyWithPages(new TablePageSource(
        "EPGP Attendance Standings",
        table,
        ImmutableList.of("player", "raids"),
        ImmutableSet.of("raids")));
  }

  @Override
  public String help() {
    return "[<...character:string>] [--raid <...raid:string>] - Displays attendance standings (optionally filtered).";
  }

  @Override
  public String command() {
    return "attendance";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
