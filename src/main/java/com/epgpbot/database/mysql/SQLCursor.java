package com.epgpbot.database.mysql;

import java.sql.ResultSet;
import java.util.Optional;

import com.epgpbot.database.Cursor;

public class SQLCursor implements Cursor {
  private final ResultSet rs;

  public SQLCursor(ResultSet rs) {
    this.rs = rs;
  }

  @Override
  public void close() throws Exception {
    rs.close();
  }

  @Override
  public boolean next() throws Exception {
    return rs.next();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getNullable(String column, Class<T> type) throws Exception {
    if (type == String.class) {
      return (Optional<T>)Optional.ofNullable(rs.getString(column));
    }

    if (type == Long.class) {
      Long value = rs.getLong(column);
      if (rs.wasNull()) {
        return Optional.empty();
      }
      return (Optional<T>)Optional.of(value);
    }

    if (type == Integer.class) {
      Integer value = rs.getInt(column);
      if (rs.wasNull()) {
        return Optional.empty();
      }
      return (Optional<T>)Optional.of(value);
    }

    if (type == Boolean.class) {
      Boolean value = rs.getBoolean(column);
      if (rs.wasNull()) {
        return Optional.empty();
      }
      return (Optional<T>)Optional.of(value);
    }

    if (type == Float.class) {
      Float value = rs.getFloat(column);
      if (rs.wasNull()) {
        return Optional.empty();
      }
      return (Optional<T>)Optional.of(value);
    }

    if (type == Double.class) {
      Double value = rs.getDouble(column);
      if (rs.wasNull()) {
        return Optional.empty();
      }
      return (Optional<T>)Optional.of(value);
    }

    throw new Exception("Unsupported type " + type.getCanonicalName());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getNullable(int column, Class<T> type) throws Exception {
    if (type == String.class) {
      return (Optional<T>)Optional.ofNullable(rs.getString(column));
    }

    if (type == Long.class) {
      Long value = rs.getLong(column);
      if (rs.wasNull()) {
        return Optional.empty();
      }
      return (Optional<T>)Optional.of(value);
    }

    if (type == Integer.class) {
      Integer value = rs.getInt(column);
      if (rs.wasNull()) {
        return Optional.empty();
      }
      return (Optional<T>)Optional.of(value);
    }

    if (type == Boolean.class) {
      Boolean value = rs.getBoolean(column);
      if (rs.wasNull()) {
        return Optional.empty();
      }
      return (Optional<T>)Optional.of(value);
    }

    if (type == Float.class) {
      Float value = rs.getFloat(column);
      if (rs.wasNull()) {
        return Optional.empty();
      }
      return (Optional<T>)Optional.of(value);
    }

    if (type == Double.class) {
      Double value = rs.getDouble(column);
      if (rs.wasNull()) {
        return Optional.empty();
      }
      return (Optional<T>)Optional.of(value);
    }

    throw new Exception("Unsupported type " + type.getCanonicalName());
  }

  @Override
  public <T> T get(String column, Class<T> type) throws Exception {
    return getNullable(column, type).get();
  }

  @Override
  public <T> T get(int column, Class<T> type) throws Exception {
    return getNullable(column, type).get();
  }
}
