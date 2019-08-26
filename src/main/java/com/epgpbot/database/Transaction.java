package com.epgpbot.database;

public interface Transaction extends AutoCloseable {
  public Statement prepare(String statement) throws Exception;
}
