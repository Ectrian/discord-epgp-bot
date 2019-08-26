package com.epgpbot.database;

public interface Database extends AutoCloseable {
  public Transaction transaction() throws Exception;
  public Transaction transaction(IsolationLevel level) throws Exception;
}
