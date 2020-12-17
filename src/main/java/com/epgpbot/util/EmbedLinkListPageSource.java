package com.epgpbot.util;

import java.util.List;

import com.epgpbot.util.MessagePaginator.PageSource;
import com.google.common.base.Joiner;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * Provides a source paged MessageEmbeds.
 */
public class EmbedLinkListPageSource implements PageSource {
  private static final int PER_PAGE = 20;
  private final String title;
  private final String titleUrl;
  private final List<String> table;

  public EmbedLinkListPageSource(String title, String titleUrl, List<String> table) {
    this.title = title;
    this.titleUrl = titleUrl;
    this.table = table;
  }

  @Override
  public int pageCount() {
    if (table.size() == 0) {
      return 1;
    }
    return (int) Math.ceil(table.size() / (double) PER_PAGE);
  }

  @Override
  public String renderPage(int page) {
    return null;
  }

  @Override
  public MessageEmbed renderPageEmbed(int page) {
    List<String> view = table.subList(
        page * PER_PAGE,
        Math.min((page + 1) * PER_PAGE, table.size())
    );

    if (page != 0) {
      while(view.size() < PER_PAGE) {
        view.add(".");  // To keep all pages the same length in # of lines.
      }
    }

    if (table.isEmpty()) {
      view.add("(no results)");
    }

    return new EmbedBuilder()
        .setAuthor(title, titleUrl)
        .setDescription(Joiner.on("\n").join(view))
        .setFooter(String.format("Page %d of %d", page + 1, this.pageCount()), null)
        .build();
  }
}
