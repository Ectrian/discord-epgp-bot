package com.epgpbot.database;

public interface Statement extends AutoCloseable {
  public Cursor executeFetch() throws Exception;
  public void executeUpdate() throws Exception;
  public long executeInsert() throws Exception;
  public <T> Statement bind(String parameter, T value) throws Exception;
  public <T> Statement bindArray(String parameter, Iterable<T> values) throws Exception;

  public static <T> String array(String parameter, Iterable<T> values) {
    int i = 0;
    String out = "(";

    for (@SuppressWarnings("unused") T value : values) {
      out += String.format(":%s_%d, ", parameter, i);
      i++;
    }

    if (i > 0) {
      out = out.substring(0, out.length() - 2);
    }

    out += ")";
    return out;
  }
}
