package com.epgpbot.armory.transport;

import java.util.concurrent.BlockingQueue;

import com.epgpbot.armory.transport.pserver.PServerArmoryAPI;
import com.epgpbot.config.Config;
import com.epgpbot.database.Database;

// Perform all armory I/O on a single thread to throttle request rate.
// Thread receives and executes ArmoryRequests and their callbacks.
public class ArmoryAPIThread extends Thread {
  private final BlockingQueue<ArmoryRequest> queue;
  private final Config config;
  private final Database db;

  public ArmoryAPIThread(Database db, Config config, BlockingQueue<ArmoryRequest> queue) {
    this.queue = queue;
    this.config = config;
    this.db = db;
  }

  @Override
  public void run() {
    ArmoryAPI api = null;

    switch (config.game_armory_type) {
      case PSERVER:
        api = new PServerArmoryAPI(config);
        break;
      case NONE:
        // fall-through, thread shouldn't be spawned when not enabled.
      case RETAIL:
      case CLASSIC:
        throw new RuntimeException("No transport for armory type " + config.game_armory_type);
    }

    while (true) {
      ArmoryRequest request = null;

      try {
        request = queue.take();
      } catch (InterruptedException e) {
        continue;
      }

      if (request.isQuit()) {
        break;
      }

      request.run(db, config, api);
    }

    System.out.printf("[Thread] Armory thread quit.\n");
  }
}
