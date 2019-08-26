package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class EPGPDecayCommandHandler extends AbstractEPGPCommandHandler {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    long count = 0;

    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare("SELECT id, ep_net, gp_net FROM players;")) {
        try (Cursor c = q.executeFetch()) {
          while (c.next()) {
            long playerId = c.get("id", Long.class);
            long oldEp = c.get("ep_net", Long.class);
            long oldGp = c.get("gp_net", Long.class);
            long newEp = context.epgp().decayEP(oldEp);
            long newGp = context.epgp().decayGP(oldGp);

            try (Statement q2 = tx.prepare("UPDATE players SET ep_net = :ep, gp_net = :gp WHERE id = :id;")) {
              q2.bind("id", playerId);
              q2.bind("ep", newEp);
              q2.bind("gp", newGp);
              q2.executeUpdate();
            }

            addDecayLogEntry(tx, playerId, getSourcePlayerId(context), newEp - oldEp, newGp - oldGp);
            count++;
          }
        }
      }
    }

    context.replyf("Operation successful - %d players updated.", count);
  }

  @Override
  public String help() {
    return "- Performs EPGP decay on all players.";
  }

  @Override
  public String command() {
    return "epgp.decay";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.DECAY_EPGP);
  }
}
