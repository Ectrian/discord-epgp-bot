package com.epgpbot.epgpbot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.transport.User;
import com.epgpbot.util.Argument;
import com.epgpbot.util.TablePageSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class TransportUidCommandHandler extends CommandHandlerAbstract {
  public void updateTable(List<Map<String, Object>> table, String argument, Optional<User> user) {
    Map<String, Object> row = new HashMap<>();

    if (user.isPresent()) {
      row.put("id", user.get().transportUserId());
      row.put("name", user.get().transportUserName());
    } else {
      row.put("id", "?");
      row.put("name", argument);
    }

    table.add(row);
  }

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    List<Map<String, Object>> table = new ArrayList<>();

    if (request.mentions().isEmpty()) {
      for (Argument arg : request.args("user")) {
        updateTable(table, arg.stringValue(), arg.userOption(context));
      }
    } else {
      for (User user : request.mentions()) {
        updateTable(table, user.transportUserName(), Optional.of(user));
      }
    }

    context.replyWithPages(new TablePageSource("User Information", table,
        ImmutableList.of("id", "name"),
        ImmutableSet.of()));
  }

  @Override
  public String help() {
    return "<...user:@ref> - Displays the Discord User ID for a given user.";
  }

  @Override
  public String command() {
    return "transport.uid";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
