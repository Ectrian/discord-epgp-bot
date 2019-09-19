package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGP;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TablePageSource;
import com.google.common.collect.ImmutableList;

public class EPGPRewriteHistoryCommandHandler extends AbstractEPGPCommandHandler {
  public long getLongArg(Request request, String name, long dflt) {
    if (!request.hasFlag(name)) {
      return dflt;
    }
    if (request.flag(name).size() != 1) {
      return dflt;
    }
    return Long.parseLong(request.flag(name).get(0));
  }

  public double getDoubleArg(Request request, String name, double dflt) {
    if (!request.hasFlag(name)) {
      return dflt;
    }
    if (request.flag(name).size() != 1) {
      return dflt;
    }
    return Double.parseDouble(request.flag(name).get(0));
  }

  // TODO: Audit logging.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> table = new ArrayList<>();
    boolean apply = request.hasFlag("go");

    long baseGp = getLongArg(request, "base-gp", context.epgp().baseGP());
    long decayRate = getLongArg(request, "decay-rate", context.epgp().decayRate());
    long decayBaseGp = getLongArg(request, "decay-gp", context.epgp().decayBaseGP());
    long initialExtraGp = getLongArg(request, "initial-gp", 0);
    double gpMultiplier = getDoubleArg(request, "gp-mult", 1.0);
    double epMultiplier = getDoubleArg(request, "ep-mult", 1.0);

    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare("SELECT id, name, ep_net, gp_net, ep_earned, gp_earned, ((SELECT COUNT(*) FROM epgp_log WHERE target_player_id = players.id AND type = :loot_type AND gp_delta > 0) - (SELECT COUNT(*) FROM epgp_log WHERE target_player_id = players.id AND type = :loot_type AND gp_delta < 0)) AS items_won FROM players;")) {
        q.bind("loot_type", EPGPEventType.LOOT.ordinal());
        try (Cursor r = q.executeFetch()) {
          while (r.next()) {
            Map<String, Object> row = new HashMap<>();
            long playerId = r.get("id", Long.class);
            String playerName = r.get("name", String.class);
            long epNet = r.get("ep_net", Long.class);
            long gpNet = r.get("gp_net", Long.class);
            row.put("id", playerId);
            row.put("name", playerName);
            row.put("ep_net", epNet);
            row.put("gp_net", gpNet);
            row.put("gp_earned", r.get("gp_earned", Long.class));
            row.put("priority", context.epgp().priority(epNet, gpNet));
            handlePlayerUpdate(
                context.epgp(),
                tx, row, playerId,
                baseGp,
                decayRate,
                decayBaseGp,
                initialExtraGp,
                epMultiplier,
                gpMultiplier,
                apply
            );
            long newEpNet = (Long)row.get("new_ep_net");
            long newGpNet = (Long)row.get("new_gp_net");
            double newPriority = (Double)row.get("new_priority");
            row.put("ep_delta", newEpNet - epNet);
            row.put("gp_delta", newGpNet - gpNet);
            row.put("priority_delta", newPriority - context.epgp().priority(epNet, gpNet));
            row.put("items_won", r.get("items_won", Long.class));

            if (epNet == 0 && gpNet == 0) {
              continue;
            }

            table.add(row);
          }
        }
      }
    }

    Comparator<Map<String, Object>> sorter = new Comparator<Map<String, Object>>() {
      @Override
      public int compare(Map<String, Object> a, Map<String, Object> b) {
        double aPriority = (double)a.get("priority");
        double bPriority = (double)b.get("priority");
        if (aPriority == bPriority) {
          String aName = (String)a.get("name");
          String bName = (String)b.get("name");
          return aName.compareTo(bName);
        }
        return (aPriority < bPriority) ? 1 : -1;
      }
    };

    table.sort(sorter);

    int i = 0;
    for (Map<String, Object> row : table) {
      row.put("old_rank", i++);
    }

    table.sort(sorter);

    i = 0;
    for (Map<String, Object> row : table) {
      int new_rank = i++;
      row.put("new_rank", new_rank);
      row.put("rank_change", new_rank - (Integer)row.get("old_rank"));
    }

    if (!apply) {
      context.replyf("**Retroactive Decay Rewrite (PREVIEW) **\nThe following is a preview of the new EPGP standings after applying new parameters retroactively. Re-run with '--go' to apply.");
    } else {
      context.replyf("**Retroactive Decay Rewrite **\nThe following changes have been applied:");
    }

    context.replyWithPages(
        new TablePageSource("EPGP Adjustments", table, ImmutableList.of(
            "id", "name",
            "ep_net", "gp_net", "priority",
            "new_ep_net", "new_gp_net", "new_priority",
            "ep_delta", "gp_delta", "priority_delta",
            "old_rank", "new_rank", "rank_change",
            "items_won", "gp_earned"
        )));

    if (apply) {
      context.replyf("Operation successful - %d players updated.", table.size());
    }
  }

  private void handlePlayerUpdate(
      EPGP epgp,
      Transaction tx,
      Map<String, Object> row,
      long playerId,
      long baseGp,
      long decayRate,
      long decayBaseGp,
      long initialExtraGp,
      double epMultiplier,
      double gpMultiplier,
      boolean apply
  ) throws Exception {
    try (Statement q = tx.prepare("SELECT * FROM epgp_log WHERE target_player_id = :player_id ORDER BY timestamp ASC;")) {
      q.bind("player_id", playerId);
      try (Cursor r = q.executeFetch()) {
        long newEpNet = 0;
        long newGpNet = initialExtraGp;

        if (initialExtraGp != 0 && apply) {
          // TODO
        }

        while (r.next()) {
          EPGPEventType type = EPGPEventType.values()[r.get("type", Integer.class)];
          long logId = r.get("id", Long.class);
          long ep = r.get("ep_delta", Long.class);
          long gp = r.get("gp_delta", Long.class);

          if (type == EPGPEventType.DECAY) {
            long oldEpNet = newEpNet;
            long oldGpNet = newGpNet;
            newEpNet = epgp.decayEP(newEpNet, decayRate);
            newGpNet = epgp.decayGP(newGpNet, decayRate, decayBaseGp);

            if (apply) {
              try (Statement u = tx.prepare("UPDATE epgp_log SET ep_delta = :ep_delta, gp_delta = :gp_delta WHERE id = :log_id;")) {
                u.bind("log_id", logId);
                u.bind("ep_delta", newEpNet - oldEpNet);
                u.bind("gp_delta", newGpNet - oldGpNet);
                u.executeUpdate();
              }
            }
            continue;
          }

          if (type == EPGPEventType.IMPORT ||
              type == EPGPEventType.INCENTIVE ||
              type == EPGPEventType.PENALTY ||
              type == EPGPEventType.RAID ||
              type == EPGPEventType.STANDBY) {
            ep = (long)Math.floor(ep * epMultiplier);
            if (apply) {
              // TODO
            }
          }

          if (type == EPGPEventType.LOOT || type == EPGPEventType.IMPORT) {
            gp = (long)Math.floor(gp * gpMultiplier);
            if (apply) {
              // TODO
            }
          }

          newEpNet += ep;
          newGpNet += gp;
        }

        row.put("new_ep_net", newEpNet);
        row.put("new_gp_net", newGpNet);
        row.put("new_priority", epgp.priority(newEpNet, newGpNet, baseGp));

        if (apply) {
          try (Statement u = tx.prepare("UPDATE players SET ep_net = :ep_net, gp_net = :gp_net WHERE id = :player_id;")) {
            u.bind("player_id", playerId);
            u.bind("ep_net", newEpNet);
            u.bind("gp_net", newGpNet);
            u.executeUpdate();
          }
        }
      }
    }
  }

  @Override
  public String help() {
    return "[--go] [--base-gp <:int>] [--decay-gp <:int>] [--decay-rate <:int>] [--initial-gp <:int>] [--ep-mult <:float>] [--gp-mult <:float>] - Adjusts EPGP values by applying new EPGP parameters retroactively.";
  }

  @Override
  public String command() {
    return "epgp.rewritehistory";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.DECAY_EPGP, PermissionType.AUDIT_EPGP);
  }
}
