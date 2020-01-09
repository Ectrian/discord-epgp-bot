package com.epgpbot.epgpbot.schema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.epgpbot.database.Transaction;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class LogEntryFormatOptions {
  boolean showEventId = false;
  boolean showTargetCharacterName = false;
  boolean showNote = false;

  public static LogEntryFormatOptions forRequest(CommandContext context, Request request, Transaction tx) {
    LogEntryFormatOptions opts = new LogEntryFormatOptions();

    if (request.hasFlag("show")) {
      Set<String> vals = new HashSet<>(request.flag("show"));

      if (vals.contains("id")) {
        opts.showEventId = true;
      }

      if (vals.contains("character")) {
        opts.showTargetCharacterName = true;
      }

      if (vals.contains("note")) {
        opts.showNote = true;
      }
    }

    return opts;
  }

  public ImmutableList<String> getColumns() {
    ArrayList<String> columns = new ArrayList<>();
    if (showEventId) {
      columns.add("id");
    }
    columns.add("date");
    columns.add("ep");
    columns.add("gp");
    columns.add("player");
    if (showTargetCharacterName) {
      columns.add("character");
    }
    columns.add("reason");
    columns.add("officer");
    if (showNote) {
      columns.add("note");
    }
    return ImmutableList.copyOf(columns);
  }

  public ImmutableSet<String> getRightJustifiedColumns() {
    HashSet<String> columns = new HashSet<>();
    if (showEventId) {
      columns.add("id");
    }
    columns.add("ep");
    columns.add("gp");
    return ImmutableSet.copyOf(columns);
  }
}