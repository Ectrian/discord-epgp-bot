package com.epgpbot.database.mysql;

import java.sql.Types;

public enum SQLNull {
  ARRAY(Types.ARRAY),
  BIGINT(Types.BIGINT),
  BINARY(Types.BINARY),
  BIT(Types.BIT),
  BLOB(Types.BLOB),
  BOOLEAN(Types.BOOLEAN),
  CHAR(Types.CHAR),
  CLOB(Types.CLOB),
  DATALINK(Types.DATALINK),
  DATE(Types.DATE),
  DECIMAL(Types.DECIMAL),
  DISTINCT(Types.DISTINCT),
  DOUBLE(Types.DOUBLE),
  FLOAT(Types.FLOAT),
  INTEGER(Types.INTEGER),
  JAVA_OBJECT(Types.JAVA_OBJECT),
  LONGNVARCHAR(Types.LONGNVARCHAR),
  LONGVARBINARY(Types.LONGVARBINARY),
  LONGVARCHAR(Types.LONGVARCHAR),
  NCHAR(Types.NCHAR),
  NCLOB(Types.NCLOB),
  NULL(Types.NULL),
  NUMERIC(Types.NUMERIC),
  NVARCHAR(Types.NVARCHAR),
  OTHER(Types.OTHER),
  REAL(Types.REAL),
  REF(Types.REF),
  REF_CURSOR(Types.REF_CURSOR),
  ROWID(Types.ROWID),
  SMALLINT(Types.SMALLINT),
  SQLXML(Types.SQLXML),
  STRUCT(Types.STRUCT),
  TIME(Types.TIME),
  TIME_WITH_TIMEZONE(Types.TIME_WITH_TIMEZONE),
  TIMESTAMP(Types.TIMESTAMP),
  TIMESTAMP_WITH_TIMEZONE(Types.TIMESTAMP_WITH_TIMEZONE),
  TINYINT(Types.TINYINT),
  VARBINARY(Types.VARBINARY),
  VARCHAR(Types.VARCHAR),
  TEXT(Types.VARCHAR),
  STRING(Types.VARCHAR);

  private final int sqlType;

  private SQLNull(int sqlType) {
    this.sqlType = sqlType;
  }

  public int getSqlType() {
    return sqlType;
  }

  public static <T> SQLNull forType(Class<T> type) {
    if (type.equals(String.class)) {
      return SQLNull.VARCHAR;
    }
    else if (type.equals(Long.class)) {
      return SQLNull.INTEGER;
    }
    else if (type.equals(Integer.class)) {
      return SQLNull.INTEGER;
    }
    else if (type.equals(Double.class)) {
      return SQLNull.DOUBLE;
    }
    else if (type.equals(Float.class)) {
      return SQLNull.FLOAT;
    }
    else if (type.equals(Boolean.class)) {
      return SQLNull.BOOLEAN;
    }
    else if (type.equals(Short.class)) {
      return SQLNull.INTEGER;
    }
    else {
      throw new IllegalArgumentException(type.getCanonicalName());
    }
  }
}
