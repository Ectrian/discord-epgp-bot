package com.epgpbot.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Provides functionality for formatting ASCII tables.
 */
public class TextTable {
  public static String repeat(String s, int n) {
    String out = "";
    for (int i = 0; i < n; i++) {
      out += s;
    }
    return out;
  }

  public static String lpad(String s, int n, String fill) {
    String out = s;
    while (out.length() < n) {
      out += fill;
    }
    return out;
  }

  public static String rpad(String s, int n, String fill) {
    String out = s;
    while (out.length() < n) {
      out = fill + out;
    }
    return out;
  }

  public static ImmutableSet<Class<?>> NUMERIC_TYPES = new ImmutableSet.Builder<Class<?>>()
      .add(Long.class)
      .add(Integer.class)
      .add(Short.class)
      .add(Float.class)
      .add(Double.class)
      .add(long.class)
      .add(int.class)
      .add(short.class)
      .add(float.class)
      .add(double.class)
      .build();

  @SuppressWarnings("unchecked")
  public static <T> String format(List<T> data) {
    if (data.isEmpty()) {
      return "(no data)";
    }

    Class<?> type = data.stream().findFirst().get().getClass();
    if (Map.class.isAssignableFrom(type)) {
      return formatListOfMaps((List<Map<String, Object>>)data);
    }

    // XXX: Is getDeclaredFields ordered consistently?
    List<String> columns = Arrays
        .stream(type.getDeclaredFields())
        .map(i -> i.getName())
        .collect(Collectors.toList());
    Set<String> columnsToRJust = Arrays
        .stream(type.getDeclaredFields())
        .filter(i -> NUMERIC_TYPES.contains(i.getType()))
        .map(i -> i.getName())
        .collect(Collectors.toSet());
    List<Map<String, Object>> data2 = data
        .stream()
        .map(i -> {
          try {
            Map<String, Object> row = new HashMap<>();
            for (Field f : i.getClass().getDeclaredFields()) {
              if (f.canAccess(i)) {
                row.put(f.getName(), f.get(i));
              }
            }
            return row;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        })
        .collect(Collectors.toList());
    return format(columns, data2, columnsToRJust);
  }

  private static String formatListOfMaps(List<Map<String, Object>> data) {
    Set<String> rjustColumns = new LinkedHashSet<>();
    Set<String> columns = new LinkedHashSet<>();

    for (Map<String, Object> row : data) {
      for (Map.Entry<String, Object> col : row.entrySet()) {
        Object value = col.getValue();

        if (value != null && value.getClass().equals(Optional.class)) {
          Optional<?> opt = (Optional<?>)value;
          if (opt.isPresent()) {
            value = opt.get();
          }
          else {
            value = null;
          }
        }

        if (value != null && NUMERIC_TYPES.contains(value.getClass())) {
          rjustColumns.add(col.getKey());
        }

        columns.add(col.getKey());
      }
    }

    return format(ImmutableList.copyOf(columns), data, rjustColumns);
  }

  public static String format(List<String> columns,
                              List<Map<String, Object>> data,
                              Set<String> columnsRjust) {
    if (data.isEmpty()) {
      return "(no data)";
    }
    if (data.get(0).size() == 0) {
      return "(no data)";
    }

    String out = "";
    Map<String, Integer> columnWidth = new HashMap<>();

    for (String key : columns) {
      columnWidth.put(key, key.length());
    }

    for (Map<String, Object> row : data) {
      for (String key : columns) {
        Object value = row.get(key);
        String s = "";

        if (value == null) {
          continue;
        }

        if (value instanceof Optional) {
          Optional<?> option = (Optional<?>)value;
          if (!option.isPresent()) {
            continue;
          }
          s = String.format("%s", option.get());
        } else {
          s = String.format("%s", value);
        }

        if (value instanceof Double) {
          s = String.format("%.4f", value);
        }

        columnWidth.put(key, Math.max(columnWidth.get(key), s.length()));
      }
    }

    String rowSep = "";

    out += "| ";
    rowSep += "+-";
    for (String key : columns) {
      out += lpad(key, columnWidth.get(key), " ");
      out += " | ";
      rowSep += repeat("-", columnWidth.get(key));
      rowSep += "-+-";
    }
    rowSep = rowSep.substring(0, rowSep.length() - 1);
    rowSep += "\n";
    out += "\n";
    out += rowSep;

    for (Map<String, Object> row : data) {
      out += "| ";
      for (String key : columns) {
        Object value = row.get(key);
        String s = "";

        if (value != null) {
          if (value instanceof Optional) {
            Optional<?> option = (Optional<?>)value;
            if (option.isPresent()) {
              s = String.format("%s", option.get());
            }
          }
          else if (value instanceof Double) {
            s = String.format("%.4f", value);
          }
          else {
            s = String.format("%s", value);
          }
        }

        if (columnsRjust.contains(key)) {
          out += rpad(s, columnWidth.get(key), " ");
        } else {
          out += lpad(s, columnWidth.get(key), " ");
        }

        out += " | ";
      }
      out += "\n";
    }

    out += rowSep;

    return rowSep + out;
  }
}
