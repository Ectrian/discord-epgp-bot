package com.epgpbot.epgpbot.commands;

import java.util.List;

import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.LogEntry;
import com.epgpbot.epgpbot.schema.LogEntryFilter;
import com.epgpbot.epgpbot.schema.LogEntryFormatOptions;
import com.epgpbot.epgpbot.schema.MergedLogEntry;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.TablePageSource;
import com.google.common.collect.ImmutableList;

public class EPGPLogV2CommandHandler extends CommandHandlerAbstract {
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    try (Transaction tx = context.database().transaction()) {
      LogEntryFilter filter = LogEntryFilter.forRequest(context, request, tx);
      LogEntryFormatOptions format = LogEntryFormatOptions.forRequest(context, request, tx);
      List<LogEntry> entries = filter.fetch(tx, 0, 5000);
      List<MergedLogEntry> mergedEntries = MergedLogEntry.fromEntries(entries, format);
      context.replyWithPages(
          new TablePageSource(
            "EPGP Log",
            MergedLogEntry.formatTable(mergedEntries, format),
            format.getColumns(),
            format.getRightJustifiedColumns()));
    }
  }

  @Override
  public String help() {
    // Undocumented: --show-hidden
    return "[<...character:string|'<all>'>] [--id <...id:int>] [--type <...type:string>] [--officer <...character:string>] [--loot <...item:string> [--show <...{id|character|note|undo}>] [--sort {time|action_time}] - Displays EPGP logs.";
  }

  @Override
  public String advancedHelp() {
    return
          "```"
        + "Filters, sorts, and displays paginated EPGP logs.\n"
        + "Only the player who issued the command and officers may page through the logs.\n"
        + "\n"
        + "<...character:string|'<all>'>:\n"
        + "  Filter the log to include only player(s) associated with the given character(s).\n"
        + "  If no character(s) are provided, defaults to the player who issued the command.\n"
        + "  The special value '<all>' will include all players.\n"
        + "[--id <...id:int>]:\n"
        + "  Filter the log to include only the given entries (by unique ID).\n"
        + "[--type <...{penalty|loot|standby|raid|incentive|decay}>]:\n"
        + "  Filter the log to include only the given event type(s).\n"
        + "[--officer <...character:string>]:\n"
        + "  Filter the log to include only actions performed by the given officer(s).\n"
        + "[--loot <...item:string>]:\n"
        + "  Filter the log to include only the specific in-game items.\n"
        + "[--show <...{id|character|note}>]:\n"
        + "  Display the given extra column(s) - by default, they are hidden.\n"
        + "  In particular, --show id is useful for getting IDs needed by !epgp.undo.\n"
        + "[--show undo]:\n"
        + "  Display UNDO's and entries that have been UNDONE - by default, they are hidden.\n"
        + "[--sort <{time|action_time}>]:\n"
        + "  Sort the log by the given field.\n"
        + "  time: When the event occurred.\n"
        + "  action_time: When the event was logged (may differ from time).\n"
        + "\n"
        + "Example: Audit the actions of a particular officer.\n"
        + "!epgp.log <all> --officer Ectrian --sort action_time\n"
        + "\n"
        + "Example: Find all players who have looted a given item.\n"
        + "!epgp.log <all> --type loot --loot 'Arcanist Mantle'\n"
        + "\n"
        + "Example: Find all items looted by a player.\n"
        + "!epgp.log Ectrian --type loot\n"
        + "```\n";
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
