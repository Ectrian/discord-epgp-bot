package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TablePageSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;

public class TransportListNoRolesCommandHandler extends CommandHandlerAbstract {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> data = new ArrayList<>();
    Guild server = context.transport().raw().getGuildById(context.config().discord_server_id);
    List<Member> members = server.getMembers();

    for (Member m : members) {
      if (!m.getRoles().isEmpty()) {
        continue;
      }

      Map<String, Object> row = new HashMap<>();
      row.put("id", m.getUser().getId());
      row.put("name",
          String.format("%s#%s",
              m.getUser().getName(),
              m.getUser().getDiscriminator()));
      row.put("alias", m.getEffectiveName());
      data.add(row);
    }


    context.replyWithPages(new TablePageSource("Unroled Members", data,
        ImmutableList.of("id", "name", "alias"),
        ImmutableSet.of()));
  }

  @Override
  public String help() {
    return " - Lists Discord users with no roles.";
  }

  @Override
  public String command() {
    return "transport.listnoroles";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_PERMISSIONS);
  }
}
