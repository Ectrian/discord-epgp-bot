package com.epgpbot.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.epgpbot.util.MessagePaginator.PageSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.dv8tion.jda.api.entities.MessageEmbed;

public class TablePageSource implements PageSource {
  private static final int PER_PAGE = 15;
  private final List<Map<String, Object>> table;
  private final ImmutableList<String> columns;
  private final Set<String> columnsToRjust;
  private final String title;
  private int perPage;

  public TablePageSource(String title, List<Map<String, Object>> table,
      ImmutableList<String> columns, Set<String> columnsToRjust) {
    this.table = table;
    this.columns = columns;
    this.columnsToRjust = columnsToRjust;
    this.title = title;
    this.perPage = PER_PAGE;
  }

  public TablePageSource(String title, List<Map<String, Object>> table,
      ImmutableList<String> columns) {
    this(title, table, columns, ImmutableSet.of());
  }

  public TablePageSource setPerPage(int perPage) {
    this.perPage = perPage;
    return this;
  }

  @Override
  public int pageCount() {
    if (table.size() == 0) {
      return 1;
    }
    return (int) Math.ceil(table.size() / (double) perPage);
  }

  @Override
  public String renderPage(int page) {
    List<Map<String, Object>> view = table.subList(
        page * perPage,
        Math.min((page + 1) * perPage, table.size())
    );

    if (page != 0) {
      // Keep all pages the same length (in terms of # of lines).
      while(view.size() < perPage) {
        Map<String, Object> row = new HashMap<>();
        for (String c : columns) {
          row.put(c, Optional.empty());
        }
        view.add(row);
      }
    }

    return String.format(
        "**%s:**\n```\n%s```\n%s",
        title,
        TextTable.format(columns, view, columnsToRjust),
        (this.pageCount() > 1) ?
            String.format("Page: %d of %d\n", page + 1, this.pageCount()) :
            "");
  }

  @Override
  public MessageEmbed renderPageEmbed(int page) {
    return null;
  }
}
