package com.epgpbot.database;

public interface Statement extends AutoCloseable {
  public Cursor executeFetch() throws Exception;
  public void executeUpdate() throws Exception;
  public long executeInsert() throws Exception;
  public <T> Statement bind(String parameter, T value) throws Exception;
  public <T> Statement bind(ScalarParameter<T> parameter, T value) throws Exception;
  public <T> Statement bind(ArrayParameter<T> parameter, Iterable<T> values) throws Exception;
  public <T> Statement bindArray(String parameter, Iterable<T> values) throws Exception;

  public static String compile(Iterable<?> parts) {
    String out = "";

    for (Object part : parts) {
      if (part instanceof String) {
        out += part;
      }
      else if (part instanceof ScalarParameter) {
        out += ":" + ((ScalarParameter<?>)part).name();
      }
      else if (part instanceof ArrayParameter) {
        out += ":" + ((ArrayParameter<?>)part).name();
      }
      else {
        throw new IllegalArgumentException(part.getClass().getCanonicalName());
      }
    }

    return out;
  }

  public static <T> String in(ArrayParameter<T> parameter, Iterable<T> values) {
    return in(parameter.name(), values);
  }

  public static <T> String in(String parameter, Iterable<T> values) {
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
