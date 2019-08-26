package com.epgpbot.epgpbot.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class EPGPLogCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    Set<EPGPEventType> types = ImmutableSet.copyOf(EPGPEventType.values());
    Set<String> loots = ImmutableSet.of();

    if (request.hasFlag("type") &&
        request.flag("type").size() > 0) {
      types = new HashSet<>();

      for (String type : request.flag("type")) {
        for (EPGPEventType e : EPGPEventType.values()) {
          if (type.toLowerCase().equals(e.toString().toLowerCase())) {
            types.add(e);
          }
        }
      }
    }

    if (request.hasFlag("loot")) {
      loots = ImmutableSet.copyOf(request.flag("loot"));
    }

    boolean showHidden = false;
    if (request.hasFlag("show-hidden") &&
        context.user().hasPermission(PermissionType.AUDIT_EPGP)) {
      showHidden = true;
    }

    if (request.arguments().isEmpty()) {
      try (Transaction tx = context.database().transaction()) {
        PlayerId player = getInferredPlayer(tx, context);
        if (player == null) {
          sendError(context, "I'm not sure who you are. Try using '!epgp.log <character>' instead.");
          return;
        }

        try (Statement q = tx.prepare(String.format(LOG_QUERY_FMT, "WHERE l.target_player_id = :player_id", EPGP_LOG_FETCH_LIMIT))) {
          q.bind("player_id", player.id);
          try (Cursor r = q.executeFetch()) {
            context.replyWithPages(formatLogEntries(r, String.format("EPGP Logs for __%s__", player.name), types, loots, showHidden));
          }
        }
      }
      return;
    }

    String characterName = ucfirst(request.arguments().get(0));

    if (characterName.equals("<all>")) {
      try (Transaction tx = context.database().transaction()) {
        try (Statement q = tx.prepare(String.format(LOG_QUERY_FMT, "", EPGP_LOG_FETCH_LIMIT))) {
          try (Cursor r = q.executeFetch()) {
            context.replyWithPages(formatLogEntries(r, "EPGP Logs for __<everyone>__", types, loots, showHidden));
          }
        }
      }
    } else {
      try (Transaction tx = context.database().transaction()) {
        PlayerId player = getPlayerIdForCharacter(tx, characterName);
        if (player == null) {
          sendError(context, "Unknown or unlinked character '%s'", ucfirst(characterName));
          return;
        }

        try (Statement q = tx.prepare(String.format(LOG_QUERY_FMT, "WHERE l.target_player_id = :player_id", EPGP_LOG_FETCH_LIMIT))) {
          q.bind("player_id", player.id);
          try (Cursor r = q.executeFetch()) {
            context.replyWithPages(formatLogEntries(r, String.format("EPGP Logs for __%s__", player.name), types, loots, showHidden));
          }
        }
      }
    }
  }

  @Override
  public String help() {
    return "[<'<all>'|character:string>] [--page <n:int>] [--limit <count:int>] [--type <...type:string>] [--loot <...loot:string>] - Displays the EPGP action log for a given character (current if omitted).";
  }

  @Override
  public String command() {
    return "epgp.log";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.VIEW_EPGP);
  }
}
