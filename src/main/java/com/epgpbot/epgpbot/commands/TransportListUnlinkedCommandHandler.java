package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TablePageSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

public class TransportListUnlinkedCommandHandler extends CommandHandlerAbstract {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> data = new ArrayList<>();
    List<Member> unlinkedMembers = new ArrayList<>();

    Guild server = context.transport().raw().getGuildById(context.config().discord_server_id);
    List<Member> members = server.getMembers();

    try (Transaction tx = context.database().transaction()) {
      for (Member m : members) {
        try (Statement q = tx.prepare("SELECT * FROM transport_users WHERE id = :id;")) {
          q.bind("id", m.getUser().getId());
          try (Cursor r = q.executeFetch()) {
            if (!r.next()) {
              if (m.getUser().getId().equals(context.transport().raw().getSelfUser().getId())) {
                continue;
              }

              Map<String, Object> row = new HashMap<>();

              row.put("id", m.getUser().getId());
              row.put("name",
                  String.format("%s#%s",
                      m.getUser().getName(),
                      m.getUser().getDiscriminator()));
              row.put("alias", m.getEffectiveName());

              unlinkedMembers.add(m);
              data.add(row);
            }
          }
        }
      }
    }


    context.replyWithPages(new TablePageSource("Unlinked Members", data,
        ImmutableList.of("id", "name", "alias"),
        ImmutableSet.of()));
  }

  @Override
  public String help() {
    return " - Lists Discord users not linked to player records.";
  }

  @Override
  public String command() {
    return "transport.listunlinked";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_PERMISSIONS);
  }
}
