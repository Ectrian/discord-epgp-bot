package com.epgpbot.database.mysql;

import java.sql.Connection;

import com.epgpbot.database.IsolationLevel;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;

public class SQLTransaction implements Transaction {
  private final Connection connection;
  private final int oldIsolationLevel;
  private final boolean oldAutoCommit;

  public SQLTransaction(IsolationLevel level, Connection connection) throws Exception {
    this.connection = connection;
    this.oldIsolationLevel = connection.getTransactionIsolation();
    this.oldAutoCommit = connection.getAutoCommit();

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
      connection.commit();
      connection.setTransactionIsolation(oldIsolationLevel);
      connection.setAutoCommit(oldAutoCommit);
    }
    catch (Exception e) {
      connection.rollback();
      connection.setTransactionIsolation(oldIsolationLevel);
      connection.setAutoCommit(oldAutoCommit);
      throw e;
    }
    finally {
      connection.close();
    }
  }

  @Override
  public Statement prepare(String statement) throws Exception {
    return new SQLStatement(connection, statement);
  }
}
