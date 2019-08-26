package com.epgpbot.transport.discord;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;

public class ReactionManager implements AutoCloseable {
  private final ConcurrentHashMap<String, ConcurrentLinkedQueue<ReactionListener>> map;
  private final SweeperThread sweeperThread;

  public interface ReactionListener {
    public void onReactionAdded(ReactionManager manager, MessageReactionAddEvent event);
    public void onReactionRemoved(ReactionManager manager, MessageReactionRemoveEvent event);
    public boolean isExpired();
  }

  class SweeperThread extends Thread {
    public volatile boolean quit;
    private final ReactionManager manager;

    public SweeperThread(ReactionManager manager) {
      super();
      this.quit = false;
      this.manager = manager;
    }

    @Override
    public void run() {
      System.out.printf("[ReactionSweeperThread] Started.\n");
      while (!quit) {
        try {
          Thread.sleep(30000);
          manager.sweep();
        } catch (InterruptedException e) {
          continue;
        }
      }
      System.out.printf("[ReactionSweeperThread] Stopped.\n");
    }
  }

  public ReactionManager() {
    this.map = new ConcurrentHashMap<>();
    this.sweeperThread = new SweeperThread(this);
    this.sweeperThread.start();
  }

  @Override
  public void close() throws InterruptedException {
    sweeperThread.quit = true;
    sweeperThread.interrupt();
    sweeperThread.join();
  }

  public void registerListener(String messageId, ReactionListener listener) {
    map.putIfAbsent(messageId, new ConcurrentLinkedQueue<>());
    ConcurrentLinkedQueue<ReactionListener> queue = map.get(messageId);
    queue.add(listener);
  }

  public void unregisterListener(String messageId, ReactionListener listener) {
    ConcurrentLinkedQueue<ReactionListener> queue = map.get(messageId);
    if (queue != null) {
      queue.remove(listener);
      if (queue.isEmpty()) {
        map.remove(messageId);
      }
    }
  }

  public void sweep() {
    long size = map.size();
    long removed = 0;

    for (Map.Entry<String, ConcurrentLinkedQueue<ReactionListener>> entry : map.entrySet()) {
      ConcurrentLinkedQueue<ReactionListener> queue = entry.getValue();
      Iterator<ReactionListener> it = queue.iterator();
      while (it.hasNext()) {
        ReactionListener listener = it.next();
        if (listener.isExpired()) {
          removed++;
          it.remove();
        }
      }
      if (queue.isEmpty()) {
        map.remove(entry.getKey());
      }
    }

    if (removed > 0) {
      System.out.printf("[ReactionSweeperThread] Swept %d -> %d (%d removed)\n", size, map.size(), removed);
    }
  }

  public void onReactionAdd(MessageReactionAddEvent event) {
    ConcurrentLinkedQueue<ReactionListener> queue = map.get(event.getMessageId());
    if (queue != null) {
      for (ReactionListener l : queue) {
        l.onReactionAdded(this, event);
      }
    }
  }

  public void onReactionRemove(MessageReactionRemoveEvent event) {
    ConcurrentLinkedQueue<ReactionListener> queue = map.get(event.getMessageId());
    if (queue != null) {
      for (ReactionListener l : queue) {
        l.onReactionRemoved(this, event);
      }
    }
  }
}
