package com.epgpbot.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.epgpbot.database.ArrayParameter;
import com.epgpbot.database.Cursor;
import com.epgpbot.database.DBField;
import com.epgpbot.database.DBIgnore;
import com.epgpbot.database.DBNullable;
import com.epgpbot.database.Database;
import com.epgpbot.database.ScalarParameter;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.database.mysql.SQLDatabase;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class SQLTest {
  static final class Param {
    public static final ScalarParameter<Long> ID = ScalarParameter.declare("id", Long.class);
    public static final ScalarParameter<Long> EP_DELTA = ScalarParameter.declare("ep_delta", Long.class);
    public static final ArrayParameter<EPGPEventType> EVENT_TYPES = ArrayParameter.declare("event_types", EPGPEventType.class);
    public static final ScalarParameter<EPGPEventType> EVENT_TYPE = ScalarParameter.declare("event_type", EPGPEventType.class);
    public static final ScalarParameter<String> NAME = ScalarParameter.declare("name", String.class);
    public static final ScalarParameter<Long> LOOT_ID = ScalarParameter.declare("loot_id", Long.class);
  }

  public static class EPGPLogRow {
    public long id;
    public long ep_delta;
    public long gp_delta;
    public @DBNullable Long undone_by;
    public @DBIgnore long erMahGawd;
    public @DBField("type") EPGPEventType eventType;

    @Override
    public String toString() {
      return String.format("EPGPLogRow{id=%d, ep_delta=%d, gp_delta=%d, type=%s}", id, ep_delta, gp_delta, eventType);
    }
  }

  public static void main(String[] args) throws Exception {
    final Set<Long> ids = ImmutableSet.of(17L, 18L, 19L);

    try (Database db = new SQLDatabase(
        "localhost",
        3306,
        "httpd",
        "httpd",
        "epgpdb_dev",
        false
    )) {
      /*try (Transaction tx = db.transaction()) {
        try (Statement q = tx.prepare("INSERT INTO loot_alias (id, name, loot_id) VALUES (", Param.ID, ", ", Param.NAME, ", ", Param.LOOT_ID, ");")) {
          q.bind(Param.ID, 1L);
          q.bind(Param.LOOT_ID, 99999999L);
          q.bind(Param.NAME, "DBTEST");
          q.executeInsert(); // Should fail.
        }
      }*/

      try (Transaction tx = db.transaction()) {
        try (Statement q = tx.prepare("SELECT * FROM epgp_log WHERE id IN " + Statement.in("ids", ids) + ";")) {
          q.bindArray("ids", ids);
          try (Cursor r = q.executeFetch()) {
            while (r.next()) {
              // System.out.format("id: %d\n", r.get("id", Long.class));
            }
          }
        }

        List<EPGPEventType> types = ImmutableList.of(EPGPEventType.STANDBY);
        try (Statement q = tx.prepare(
            "SELECT l.* FROM epgp_log AS l WHERE ep_delta >= ", Param.EP_DELTA, " AND type IN ", Statement.in(Param.EVENT_TYPES, types), " LIMIT 3;")) {
          q.bind(Param.EP_DELTA, 20L);
          q.bind(Param.EVENT_TYPES, types);
          try (Cursor r = q.executeFetch()) {
            while (r.next()) {
              System.out.format("(1) id: %d %d\n", r.get(Param.ID), r.get(Param.EP_DELTA));
            }
          }
          try (Cursor r = q.executeFetch()) {
            while (r.next()) {
              EPGPLogRow row = r.scan(new EPGPLogRow());
              System.out.format("(2) id: %d %d %s\n", row.id, row.ep_delta, row.eventType);
            }
          }
          try (Cursor r = q.executeFetch()) {
            for (EPGPLogRow row : r.iterate(new EPGPLogRow())) {
              System.out.format("(3) id: %d %d %s\n", row.id, row.ep_delta, row.eventType);
            }
          }
          try (Cursor r = q.executeFetch()) {
            for (Cursor row : r) {
              System.out.format("(4) id: %d %d\n", row.get(Param.ID), row.get(Param.EP_DELTA));
            }
          }
          try (Cursor r = q.executeFetch()) {
            long sum = r.stream().mapToLong((row) -> {
              try {
                return row.get(Param.EP_DELTA);
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }).sum();
            System.out.format("(5) sum=%d\n", sum);
          }
          try (Cursor r = q.executeFetch()) {
            long sum = r.stream(new EPGPLogRow()).mapToLong((row) -> row.ep_delta).sum();
            System.out.format("(5) sum=%d\n", sum);
          }
        }
      }

      try (Transaction tx = db.transaction()) {
        try (Statement q = tx.prepare("SELECT * FROM epgp_log WHERE type = ", Param.EVENT_TYPE, ";")) {
          q.bind(Param.EVENT_TYPE, EPGPEventType.LOOT);
          try (Cursor r = q.executeFetch()) {
            long sum = r.stream(new EPGPLogRow()).mapToLong(row -> row.gp_delta).sum();
            System.out.format("Total GP Earned: %d\n", sum);
          }
        }
      }

      List<EPGPLogRow> rows;
      try (Transaction tx = db.transaction()) {
        try (Statement q = tx.prepare("SELECT * FROM epgp_log WHERE type = ", Param.EVENT_TYPE, " LIMIT 5;")) {
          q.bind(Param.EVENT_TYPE, EPGPEventType.LOOT);
          try (Cursor r = q.executeFetch()) {
            rows = r.toList(EPGPLogRow::new);
          }
        }
      }
      System.out.format("rows: %s\n", rows);
      System.out.println(TextTable.format(rows));

      List<Map<String, Object>> rows2;
      try (Transaction tx = db.transaction()) {
        try (Statement q = tx.prepare("SELECT * FROM epgp_log WHERE type = ", Param.EVENT_TYPE, " LIMIT 5;")) {
          q.bind(Param.EVENT_TYPE, EPGPEventType.LOOT);
          try (Cursor r = q.executeFetch()) {
            rows2 = r.toList();
          }
        }
      }
      System.out.format("rows: %s\n", rows2);
      System.out.println(TextTable.format(rows2));
    }
  }
}
