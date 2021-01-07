package com.epgpbot.database.mysql;

import java.sql.Connection;

import com.epgpbot.database.AbstractTransaction;
import com.epgpbot.database.IsolationLevel;
import com.epgpbot.database.Statement;

public class SQLTransaction extends AbstractTransaction {
  private final Connection connection;
  private final int oldIsolationLevel;
  private final boolean oldAutoCommit;
  private boolean isFailed;

  public SQLTransaction(IsolationLevel level, Connection connection) throws Exception {
    this.connection = connection;
    this.oldIsolationLevel = connection.getTransactionIsolation();
    this.oldAutoCommit = connection.getAutoCommit();
    this.isFailed = false;

    connection.setAutoCommit(false);

    switch (level) {
      case READ_COMMITTED:
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        break;
      case REPEATABLE_READ:
        connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        break;
      case READ_UNCOMMITTED:
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        break;
      case SERIALIZABLE:
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        break;
      case DEFAULT:
        break;
    }
  }

  public SQLTransaction(Connection connection) throws Exception {
    this(IsolationLevel.DEFAULT, connection);
  }

  @Override
  public void close() throws Exception {
    try {
      if (isFailed) {
        connection.rollback();
      } else {
        connection.commit();
      }
      connection.setTransactionIsolation(oldIsolationLevel);
      connection.setAutoCommit(oldAutoCommit);
    }
    finally {
      connection.close();
    }
  }

  @Override
  public Statement prepare(String statement) throws Exception {
    try {
      return new SQLStatement(this, statement);
    } catch (Exception e) {
      setFailed(e);
      throw e;
    }
  }

  void setFailed(Exception e) {
    setFailed();
  }

  void setFailed() {
    isFailed = true;
  }

  Connection connection() {
    return connection;
  }

  @Override
  public void abort() {
    setFailed();
  }
}
