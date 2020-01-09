package com.epgpbot.database;

public abstract class AbstractDatabase implements Database {
  @Override
  public Transaction transaction() throws Exception {
    return transaction(IsolationLevel.DEFAULT);
  }
}
