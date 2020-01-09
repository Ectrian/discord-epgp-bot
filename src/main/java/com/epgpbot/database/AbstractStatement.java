package com.epgpbot.database;

public abstract class AbstractStatement implements Statement {
  @Override
  public <T> Statement bind(ScalarParameter<T> parameter, T value) throws Exception {
    return bind(parameter.name(), value);
  }

  @Override
  public <T> Statement bind(ArrayParameter<T> parameter, Iterable<T> values) throws Exception {
    return bindArray(parameter.name(), values);
  }

  @Override
  public <T> Statement bindArray(String parameter, Iterable<T> values) throws Exception {
    int i = 0;

    for (T value : values) {
      bind(String.format("%s_%d", parameter, i), value);
      i++;
    }

    return this;
  }
}
