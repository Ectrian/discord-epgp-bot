package com.epgpbot.database;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface Cursor extends AutoCloseable, Iterable<Cursor> {
  public boolean next() throws Exception;

  public <T> T get(String column, Class<T> type) throws Exception;
  public <T> T get(int column, Class<T> type) throws Exception;
  public <T> T get(ScalarParameter<T> parameter) throws Exception;

  public <T> Optional<T> getNullable(String column, Class<T> type) throws Exception;
  public <T> Optional<T> getNullable(int column, Class<T> type) throws Exception;
  public <T> Optional<T> getNullable(ScalarParameter<T> parameter) throws Exception;

  public <T> List<T> toList(Supplier<T> supplier) throws Exception;
  public List<Map<String, Object>> toList() throws Exception;

  public <T> T scan(Supplier<T> supplier) throws Exception;
  public <T> T scan(T object) throws Exception;

  public <T> Iterable<T> iterate(Supplier<T> supplier);
  public <T> Iterable<T> iterate(T object);

  public <T> Stream<T> stream(Supplier<T> supplier);
  public <T> Stream<T> stream(T object);

  public Stream<Cursor> stream();
}
