package com.epgpbot.database.mysql;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.epgpbot.database.AbstractCursor;

public class SQLCursor extends AbstractCursor {
  private final SQLTransaction tx;
  private final ResultSet rs;

  public SQLCursor(SQLTransaction tx, ResultSet rs) {
    this.tx = tx;
    this.rs = rs;
  }

  @Override
  public void close() throws Exception {
    try {
      rs.close();
    } catch (Exception e) {
      tx.setFailed(e);
      throw e;
    }
  }

  @Override
  public boolean next() throws Exception {
    try {
      return rs.next();
    } catch (Exception e) {
      tx.setFailed(e);
      throw e;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getNullable(String column, Class<T> type) throws Exception {
    try {
      if (type == String.class) {
        return (Optional<T>)Optional.ofNullable(rs.getString(column));
      }

      if (type == Long.class || type == long.class) {
        Long value = rs.getLong(column);
        if (rs.wasNull()) {
          return Optional.empty();
        }
        return (Optional<T>)Optional.of(value);
      }

      if (type == Integer.class || type == int.class) {
        Integer value = rs.getInt(column);
        if (rs.wasNull()) {
          return Optional.empty();
        }
        return (Optional<T>)Optional.of(value);
      }

      if (type == Boolean.class || type == boolean.class) {
        Boolean value = rs.getBoolean(column);
        if (rs.wasNull()) {
          return Optional.empty();
        }
        return (Optional<T>)Optional.of(value);
      }

      if (type == Float.class || type == float.class) {
        Float value = rs.getFloat(column);
        if (rs.wasNull()) {
          return Optional.empty();
        }
        return (Optional<T>)Optional.of(value);
      }

      if (type == Double.class || type == double.class) {
        Double value = rs.getDouble(column);
        if (rs.wasNull()) {
          return Optional.empty();
        }
        return (Optional<T>)Optional.of(value);
      }

      throw new Exception("Unsupported type " + type.getCanonicalName());
    } catch (Exception e) {
      tx.setFailed(e);
      throw e;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<T> getNullable(int column, Class<T> type) throws Exception {
    try {
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
    } catch (Exception e) {
      tx.setFailed(e);
      throw e;
    }
  }

  @Override
  protected void setFailed(Exception e) {
    tx.setFailed(e);
  }

  @Override
  public List<Map<String, Object>> toList() throws Exception {
    try {
      List<Map<String, Object>> out = new ArrayList<>();
      while (rs.next()) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
          row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
        }
        out.add(row);
      }
      return out;
    } catch (Exception e) {
      tx.setFailed(e);
      throw e;
    }
  }
}
