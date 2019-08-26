package com.epgpbot.database;

public interface Statement extends AutoCloseable {
  public Cursor executeFetch() throws Exception;
  public void executeUpdate() throws Exception;
  public long executeInsert() throws Exception;
  public <T> Statement bind(String parameter, T value) throws Exception;
}
