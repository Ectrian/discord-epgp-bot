package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.List;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class EPGPIntegrityCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<String> inconsistentPlayers = new ArrayList<>();

    // Check that the log entries correctly correspond with ep_net, gp_net.
    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare(
          "SELECT l.target_player_id, p.name, sum(l.ep_delta), sum(l.gp_delta), p.ep_net, p.gp_net " +
          "FROM epgp_log AS l " +
          "LEFT JOIN players AS p ON p.id = l.target_player_id " +
          "GROUP BY target_player_id " +
          "HAVING sum(l.ep_delta) != p.ep_net OR sum(l.gp_delta) != p.gp_net;")) {
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            inconsistentPlayers.add(r.get("name", String.class));
          }
        }
      }
    }

    // Check that the log entries correctly correspond with ep_earned, gp_earned.
    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare(
          "SELECT l.target_player_id, p.name, sum(l.ep_delta), sum(l.gp_delta), p.ep_earned, p.gp_earned " +
          "FROM epgp_log AS l " +
          "LEFT JOIN players AS p ON p.id = l.target_player_id " +
          "WHERE l.type != :decay_type " +
          "GROUP BY target_player_id " +
          "HAVING sum(l.ep_delta) != p.ep_earned OR sum(l.gp_delta) != p.gp_earned;")) {
        q.bind("decay_type", EPGPEventType.DECAY.ordinal());
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            inconsistentPlayers.add(r.get("name", String.class));
          }
        }
      }
    }

    if (inconsistentPlayers.isEmpty()) {
      context.reply("Database integrity verified.");
    } else {
      context.replyf("ERROR: The following players appear to be inconsistent: %s", Joiner.on(", ").join(inconsistentPlayers));
    }
  }

  @Override
  public String help() {
    return "- Checks database integrity.";
  }

  @Override
  public String command() {
    return "epgp.checkdbintegrity";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.AUDIT_EPGP);
  }
}
