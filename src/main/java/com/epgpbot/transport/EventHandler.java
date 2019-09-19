package com.epgpbot.transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.epgpbot.armory.transport.ArmoryAPIThread;
import com.epgpbot.armory.transport.ArmoryRequest;
import com.epgpbot.config.ArmoryType;
import com.epgpbot.config.Config;
import com.epgpbot.database.Database;
import com.epgpbot.epgpbot.commands.CharacterAltsCommandHandler;
import com.epgpbot.epgpbot.commands.CharacterListCommandHandler;
import com.epgpbot.epgpbot.commands.CharacterSyncCommandHandler;
import com.epgpbot.epgpbot.commands.CharacterUpdateCommandHandler;
import com.epgpbot.epgpbot.commands.CharacterWhoCanPlayCommandHandler;
import com.epgpbot.epgpbot.commands.CharacterWhoisCommandHandler;
import com.epgpbot.epgpbot.commands.CommandHandler;
import com.epgpbot.epgpbot.commands.EPGPAdjustCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPAwardIncentiveCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPAwardLootCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPAwardRaidCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPAwardStandbyCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPCompareCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPRewriteHistoryCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPDecayCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPDockCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPImportCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPCheckDBIntegrityCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPCompareInteractiveCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPLogCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPOfficerLogCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPTopDockedCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPStandingsCommandHandler;
import com.epgpbot.epgpbot.commands.EPGPTotalsCommandHandler;
import com.epgpbot.epgpbot.commands.GuildOfficersCommandHandler;
import com.epgpbot.epgpbot.commands.GuildMembersCommandHandler;
import com.epgpbot.epgpbot.commands.GuildSyncCommandHandler;
import com.epgpbot.epgpbot.commands.HelpCommandHandler;
import com.epgpbot.epgpbot.commands.InvinciblesStandingCommandHandler;
import com.epgpbot.epgpbot.commands.ItemLinkCommandHandler;
import com.epgpbot.epgpbot.commands.ItemSearchCommandHandler;
import com.epgpbot.epgpbot.commands.ItemAliasCommandHandler;
import com.epgpbot.epgpbot.commands.ItemMergeCommandHandler;
import com.epgpbot.epgpbot.commands.ItemStatsCommandHandler;
import com.epgpbot.epgpbot.commands.TransportPermissionsCommandHandler;
import com.epgpbot.epgpbot.commands.PlayerAddCommandHandler;
import com.epgpbot.epgpbot.commands.PlayerCharacterAddCommandHandler;
import com.epgpbot.epgpbot.commands.PlayerCharacterListCommandHandler;
import com.epgpbot.epgpbot.commands.PlayerCharacterOnlineCommandHandler;
import com.epgpbot.epgpbot.commands.PlayerTransportLinkCommandHandler;
import com.epgpbot.epgpbot.commands.PlayerSyncCommandHandler;
import com.epgpbot.epgpbot.commands.PlayerTransportListCommandHandler;
import com.epgpbot.epgpbot.commands.QuitCommandHandler;
import com.epgpbot.epgpbot.commands.RollCommandHandler;
import com.epgpbot.epgpbot.commands.TestCommandHandler;
import com.epgpbot.epgpbot.commands.TransportListNoRolesCommandHandler;
import com.epgpbot.epgpbot.commands.TransportListUnlinkedCommandHandler;
import com.epgpbot.epgpbot.commands.TransportWhoisCommandHandler;
import com.epgpbot.epgpbot.schema.game.Expansion;
import com.epgpbot.util.CommandParser;

public class EventHandler implements AutoCloseable {
  private final List<CommandHandler> handlers;
  private final ArmoryAPIThread armoryThread;
  private final BlockingQueue<ArmoryRequest> armoryQueue;
  private final Database db;
  private final Config config;

  public EventHandler(Database db, Config config) throws Exception {
    this.config = config;
    this.db = db;
    this.handlers = new ArrayList<>();
    this.registerHandlers(handlers);

    if (config.game_armory_type != ArmoryType.NONE) {
      this.armoryQueue = new ArrayBlockingQueue<ArmoryRequest>(100);
      this.armoryThread = new ArmoryAPIThread(this.db, this.config, this.armoryQueue);
      this.armoryThread.start();
    } else {
      this.armoryQueue = null;
      this.armoryThread = null;
    }
  }

  public Config config() {
    return this.config;
  }

  protected void registerHandlers(List<CommandHandler> handlers) {
    handlers.add(new CharacterWhoisCommandHandler());
    handlers.add(new CharacterWhoCanPlayCommandHandler());
    handlers.add(new CharacterListCommandHandler());
    handlers.add(new CharacterAltsCommandHandler());
    if (config.game_armory_type == ArmoryType.NONE) {
      handlers.add(new CharacterUpdateCommandHandler());
    }

    handlers.add(new TestCommandHandler());

    handlers.add(new HelpCommandHandler(handlers));

    handlers.add(new ItemLinkCommandHandler());
    handlers.add(new ItemSearchCommandHandler());

    if (config.game_expansion == Expansion.WRATH_OF_THE_LICH_KING) {
      handlers.add(new InvinciblesStandingCommandHandler());
    }

    handlers.add(new ItemMergeCommandHandler());
    handlers.add(new ItemAliasCommandHandler());
    handlers.add(new ItemStatsCommandHandler());

    handlers.add(new PlayerAddCommandHandler());
    handlers.add(new PlayerTransportLinkCommandHandler());
    handlers.add(new PlayerCharacterAddCommandHandler());
    handlers.add(new PlayerCharacterListCommandHandler());
    handlers.add(new PlayerTransportListCommandHandler());

    handlers.add(new QuitCommandHandler());

    handlers.add(new RollCommandHandler());

    handlers.add(new TransportWhoisCommandHandler());
    handlers.add(new TransportListUnlinkedCommandHandler());
    handlers.add(new TransportListNoRolesCommandHandler());
    handlers.add(new TransportPermissionsCommandHandler());

    if (config.game_armory_type != ArmoryType.NONE) {
      handlers.add(new CharacterSyncCommandHandler());
      handlers.add(new GuildOfficersCommandHandler());
      handlers.add(new GuildMembersCommandHandler());
      handlers.add(new GuildSyncCommandHandler());
      handlers.add(new PlayerCharacterOnlineCommandHandler());
      handlers.add(new PlayerSyncCommandHandler());
    }

    switch (config.loot_method) {
      case EPGP:
        handlers.add(new EPGPCommandHandler());
        handlers.add(new EPGPDecayCommandHandler());
        handlers.add(new EPGPDockCommandHandler());
        handlers.add(new EPGPAwardLootCommandHandler());
        handlers.add(new EPGPAwardRaidCommandHandler());
        handlers.add(new EPGPAwardIncentiveCommandHandler());
        handlers.add(new EPGPAwardStandbyCommandHandler());
        handlers.add(new EPGPLogCommandHandler());
        handlers.add(new EPGPOfficerLogCommandHandler());
        handlers.add(new EPGPStandingsCommandHandler());
        handlers.add(new EPGPTotalsCommandHandler());
        handlers.add(new EPGPCompareCommandHandler());
        handlers.add(new EPGPImportCommandHandler());
        handlers.add(new EPGPCompareInteractiveCommandHandler());
        handlers.add(new EPGPRewriteHistoryCommandHandler());
        handlers.add(new EPGPCheckDBIntegrityCommandHandler());
        handlers.add(new EPGPAdjustCommandHandler());
        handlers.add(new EPGPTopDockedCommandHandler());
        break;
      case DKP:
        break;
    }
  }

  @Override
  public void close() throws Exception {
    if (config.game_armory_type != ArmoryType.NONE) {
      armoryQueue.put(new ArmoryRequest().setQuit());
      armoryThread.join();
    }
  }

  public Database getDatabase() {
    return this.db;
  }

  public void handleReactionAdded(
      Transport transport,
      Channel source,
      User user,
      String reaction) {

  }

  public void handleIncomingMessage(
      Transport transport,
      Channel source,
      User user,
      String message,
      List<User> mentions) throws IOException {
    CommandContext context = new CommandContext(db, config, transport, source, user, armoryQueue);

    if (!message.startsWith("!")) {
      return;
    }

    Request request = null;
    try {
      request = new Request(message, mentions);
    } catch (CommandParser.ParseException e) {
      source.reply(String.format("I am unable to parse your command: %s", e.getMessage()));
      return;
    } catch (Exception e) {
      source.reply("An internal error occurred processing your request. Please try again later.");
      e.printStackTrace(System.err);
      return;
    }

    CommandHandler handler = null;
    for (CommandHandler h : handlers) {
      if (h.command().equals(request.command())) {
        handler = h;
      }
    }
    if (handler == null) {
      source.reply(String.format(
          "Unknown command '%s' - type !help to see a list of valid commands.", request.command()));
      return;
    }

    if (!user.hasPermissions(handler.permissions())) {
      source.reply("Sorry - you don't have permission to use that command.");
      return;
    }

    try {
      handler.handle(context, request);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      source.reply("An internal error occurred processing your request. Please try again later.");
      e.printStackTrace(System.err);
      return;
    }
  }
}
