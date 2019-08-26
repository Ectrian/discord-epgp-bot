package com.epgpbot.database;

import java.util.Optional;

public interface Cursor extends AutoCloseable {
  public boolean next() throws Exception;
  public <T> T get(String column, Class<T> type) throws Exception;
  public <T> T get(int column, Class<T> type) throws Exception;
  public <T> Optional<T> getNullable(String column, Class<T> type) throws Exception;
  public <T> Optional<T> getNullable(int column, Class<T> type) throws Exception;
}
