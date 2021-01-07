package com.epgpbot.database;

public interface Transaction extends AutoCloseable {
  public Statement prepare(String statement) throws Exception;
  public Statement prepare(Iterable<?> parts) throws Exception;
  public Statement prepare(Object... parts) throws Exception;

  /**
   * Abort the transaction, rolling back any changes made.
   */
  public void abort();
}
