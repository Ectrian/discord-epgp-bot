package com.epgpbot.database.mysql;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;

public class SQLStatement implements Statement {
  private final PreparedStatement statement;
  private final Map<String, int[]> parameters;

  public SQLStatement(Connection connection, String statement) throws SQLException {
    this.parameters = new HashMap<>();
    this.statement = connection.prepareStatement(parse(statement, this.parameters),
        java.sql.Statement.RETURN_GENERATED_KEYS);
  }

  @Override
  public void close() throws Exception {
    statement.close();
  }

  @Override
  public Cursor executeFetch() throws Exception {
    return new SQLCursor(statement.executeQuery());
  }

  @Override
  public void executeUpdate() throws Exception {
    statement.executeUpdate();
  }

  @Override
  public long executeInsert() throws Exception {
    statement.executeUpdate();

    try (ResultSet rs = statement.getGeneratedKeys()) {
      if (rs.next()) {
        return rs.getLong(1);
      }
    }

    return -1;
  }

  private <T> void bind(int parameter, T value) throws SQLException {
    if (value instanceof SQLNull) {
      statement.setNull(parameter, ((SQLNull) value).getSqlType());
    } else if (value instanceof String) {
      statement.setString(parameter, (String) value);
    } else if (value instanceof Integer) {
      statement.setInt(parameter, (Integer) value);
    } else if (value instanceof Long) {
      statement.setLong(parameter, (Long) value);
    } else if (value instanceof Boolean) {
      statement.setBoolean(parameter, (Boolean) value);
    } else if (value instanceof Double) {
      statement.setDouble(parameter, (Double) value);
    } else if (value instanceof Float) {
      statement.setFloat(parameter, (Float) value);
    } else if (value instanceof BigDecimal) {
      statement.setBigDecimal(parameter, (BigDecimal) value);
    } else if (value instanceof Date) {
      statement.setDate(parameter, (Date) value);
    } else if (value instanceof Timestamp) {
      statement.setTimestamp(parameter, (Timestamp) value);
    } else if (value instanceof Time) {
      statement.setTime(parameter, (Time) value);
    } else if (value instanceof InputStream) {
      statement.setBinaryStream(parameter, (InputStream) value);
    } else {
      statement.setObject(parameter, value);
    }
  }

  @Override
  public <T> Statement bind(String parameter, T value) throws Exception {
    int[] indexes = parameters.get(parameter);

    if (indexes == null) {
      throw new IllegalArgumentException("Unknown parameter: " + parameter);
    }

    for (int i : indexes) {
      bind(i, value);
    }

    return this;
  }

  private static final String parse(String query, Map<String, int[]> paramMap) {
    Map<String, LinkedList<Integer>> intermediateParams = new HashMap<>();

    int length = query.length();
    StringBuffer parsedQuery = new StringBuffer(length);
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    int index = 1;

    for (int i = 0; i < length; i++) {
      char c = query.charAt(i);

      if (inSingleQuote) {
        if (c == '\'') {
          inSingleQuote = false;
        }
      } else if (inDoubleQuote) {
        if (c == '"') {
          inDoubleQuote = false;
        }
      } else {
        if (c == '\'') {
          inSingleQuote = true;
        } else if (c == '"') {
          inDoubleQuote = true;
        } else if (c == ':' && i + 1 < length
            && Character.isJavaIdentifierStart(query.charAt(i + 1))) {
          int j = i + 2;

          while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) {
            j++;
          }

          String name = query.substring(i + 1, j);
          c = '?'; // replace the parameter with a question mark
          i += name.length(); // skip past the end if the parameter

          LinkedList<Integer> indexList = intermediateParams.get(name);

          if (indexList == null) {
            indexList = new LinkedList<Integer>();
            intermediateParams.put(name, indexList);
          }

          indexList.add(index);

          index++;
        }
      }

      parsedQuery.append(c);
    }

    // replace the lists of Integer objects with arrays of ints
    for (Iterator<Map.Entry<String, LinkedList<Integer>>> itr =
        intermediateParams.entrySet().iterator(); itr.hasNext();) {
      Map.Entry<String, LinkedList<Integer>> entry = itr.next();
      LinkedList<Integer> list = entry.getValue();
      int[] indexes = new int[list.size()];
      int i = 0;

      for (Iterator<Integer> itr2 = list.iterator(); itr2.hasNext();) {
        Integer x = itr2.next();
        indexes[i++] = x.intValue();
      }

      paramMap.put(entry.getKey(), indexes);
    }

    return parsedQuery.toString();
  }
}
