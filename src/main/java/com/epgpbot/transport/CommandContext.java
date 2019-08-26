package com.epgpbot.transport;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

import com.epgpbot.armory.transport.ArmoryRequest;
import com.epgpbot.config.Config;
import com.epgpbot.database.Database;
import com.epgpbot.epgpbot.schema.EPGP;
import com.epgpbot.util.MessagePaginator;
import com.epgpbot.util.MessagePaginator.PageSource;

public class CommandContext {
  private final Database db;
  private final Config config;
  private final Transport transport;
  private final Channel source;
  private final User user;
  private final BlockingQueue<ArmoryRequest> armoryQueue;

  public CommandContext(
      Database db,
      Config config,
      Transport transport,
      Channel source,
      User user,
      BlockingQueue<ArmoryRequest> armoryQueue) {
    this.db = db;
    this.config = config;
    this.transport = transport;
    this.source = source;
    this.user = user;
    this.armoryQueue = armoryQueue;
  }

  public EPGP epgp() {
    return new EPGP(config);
  }

  public BlockingQueue<ArmoryRequest> armory() {
    return this.armoryQueue;
  }

  public Config config() {
    return this.config;
  }

  public Database database() {
    return this.db;
  }

  public User user() {
    return this.user;
  }

  public Transport transport() {
    return this.transport;
  }

  public Channel source() {
    return this.source;
  }

  public void replyWithPages(PageSource source) {
    MessagePaginator.sendPaginatedMessage(this, source);
  }

  public void reply(String message) throws IOException {
    source.reply(message);
  }

  public void replyf(String message, Object ...args) throws IOException {
    source.reply(String.format(message, args));
  }
}
