package com.epgpbot.database;

import com.google.common.collect.ImmutableList;

public abstract class AbstractTransaction implements Transaction {
  @Override
  public Statement prepare(Iterable<?> parts) throws Exception {
    return prepare(Statement.compile(parts));
  }

  @Override
  public Statement prepare(Object ...parts) throws Exception {
    return prepare(ImmutableList.copyOf(parts));
  }
}
