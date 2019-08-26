package com.epgpbot.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
