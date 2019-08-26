package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.collect.ImmutableList;

public class TestCommandHandler extends CommandHandlerAbstract {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    /*
    List<Map<String, Object>> table = new ArrayList<>();

    for (int i = 0; i < 78; i++) {
      Map<String, Object> row = new HashMap<>();
      row.put("entry_id", i);
      table.add(row);
    }

    MessagePaginator.sendPaginatedMessage(context, new TablePageSource("Test Data", table, ImmutableList.of("entry_id")));
    */

    context.replyf("Test reference <@%s>", context.user().transportUserId());
  }

  @Override
  public String help() {
    return " - For testing.";
  }

  @Override
  public String command() {
    return "test";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.AUDIT_EPGP);
  }

  @Override
  public boolean visible() {
    return false;
  }
}
