package com.epgpbot.database.mysql;

import com.epgpbot.database.Database;
import com.epgpbot.database.IsolationLevel;
import com.epgpbot.database.Transaction;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class SQLDatabase implements Database {
  private final ComboPooledDataSource source;

  public SQLDatabase(
      String host,
      int port,
      String username,
      String password,
      String name,
      boolean isSSL) throws Exception {
    ComboPooledDataSource source = new ComboPooledDataSource();
    String url = String.format("jdbc:mysql://%s:%d/%s", host, port, name);

    if (isSSL) {
      url += "?verifyServerCertificate=true&useSSL=true&requireSSL=true";
    } else {
      url += "?useSSL=false";
    }

    source.setJdbcUrl(url);
    source.setDriverClass("com.mysql.cj.jdbc.Driver");
    source.setUser(username);
    source.setPassword(password);
    source.setTestConnectionOnCheckin(false);
    source.setTestConnectionOnCheckout(false);  // Too expensive.
    source.setDebugUnreturnedConnectionStackTraces(true);
    source.setInitialPoolSize(2);
    source.setAcquireIncrement(2);
    source.setMinPoolSize(2);
    source.setMaxPoolSize(16);
    source.setAutoCommitOnClose(true);
    source.setCheckoutTimeout(5000);  // in ms
    source.setAcquireRetryAttempts(5);
    source.setAcquireRetryDelay(1000);  // in ms
    source.setMaxConnectionAge(300);  // in sec
    source.setMaxIdleTime(300);  // in sec
    source.setIdleConnectionTestPeriod(30);  // in sec
    source.setUnreturnedConnectionTimeout(600);  // in sec
    source.setMaxIdleTimeExcessConnections(600);  // in sec
    this.source = source;
  }

  @Override
  public void close() throws Exception {
    source.close();
  }

  @Override
  public Transaction transaction() throws Exception {
    return transaction(IsolationLevel.DEFAULT);
  }

  @Override
  public Transaction transaction(IsolationLevel level) throws Exception {
    return new SQLTransaction(source.getConnection());
  }
}
