package com.epgpbot.util;

import java.util.ArrayList;

/**
 * Discord imposes a maximum message length of 2k characters.
 * Thus, messages >2k characters must be chunked into multiple messages.
 * However, since messages are markdown, a trivial algorithm does not work.
 * For example, if a chunk boundary lies in a markdown code block (``` text ```).
 * We also want to make a good effort to place chunk boundaries on line breaks,
 * wherever possible, to make the effect invisible to the user.
 */
public class TextChunker {
  enum State {
    TOKEN,
  }

  public static Iterable<String> tokenize(String m) {
    ArrayList<String> tokens = new ArrayList<>();
    State state = State.TOKEN;
    StringBuilder token = new StringBuilder();
    String buf = "";
    int i = 0;

    parse: while (true) {
      switch (state) {
        case TOKEN: {
          if (i >= m.length()) {
            if (buf.equals("```")) {
              if (token.length() > 0) {
                tokens.add(token.toString());
                token.setLength(0);
              }
              tokens.add(buf + "\n");
              buf = "";
            }
            if (buf.length() > 0) {
              token.append(buf);
              buf = "";
            }
            if (token.length() > 0) {
              tokens.add(token.toString());
              token.setLength(0);
            }
            break parse;
          }

          char c = m.charAt(i);
          switch (c) {
            case '`': {
              buf += c;
              i++;
              continue;
            }

            case '\n': {
              buf += c;
              i++;

              if (buf.equals("```\n")) {
                if (token.length() > 0) {
                  tokens.add(token.toString());
                  token.setLength(0);
                }
                tokens.add(buf);
                buf = "";
              }
              if (buf.length() > 0) {
                token.append(buf);
                buf = "";
              }
              if (token.length() > 0) {
                tokens.add(token.toString());
                token.setLength(0);
              }
              continue;
            }

            default: {
              if (buf.length() > 0) {
                token.append(buf);
                buf = "";
              }
              token.append(c);
              i++;
              continue;
            }
          }
        }
      }
    }

    return tokens;
  }

  public static Iterable<String> chunk(String m, int chunkMaxSize) {
    assert chunkMaxSize > 6;
    ArrayList<String> chunks = new ArrayList<>();
    StringBuilder chunk = new StringBuilder();
    boolean inCodeBlock = false;
    int codeBlockSize = 0;

    for (String token : tokenize(m)) {
      int neededSpace = token.length() + (inCodeBlock ? "```\n".length() : 0);

      if (chunk.length() + neededSpace > chunkMaxSize) {
        if (inCodeBlock) {
          if (codeBlockSize == 0) {
            chunk.setLength(chunk.length() - "```\n".length());
          } else {
            chunk.append("```\n");
          }
        }
        if (chunk.length() > 0) {
          chunks.add(chunk.toString());
          chunk.setLength(0);
        }

        if (inCodeBlock) {
          chunk.append("```\n");
          codeBlockSize = 0;
        }
      }

      if (token.equals("```\n")) {
        inCodeBlock = !inCodeBlock;
        if (inCodeBlock) {
          codeBlockSize = 0;
          chunk.append(token);
        } else {
          if (codeBlockSize == 0) {
            chunk.setLength(chunk.length() - "```\n".length());
          } else {
            chunk.append("```\n");
          }
        }
      } else {
        int maxLine = chunkMaxSize - 2 * "```\n".length();
        while (token.length() > maxLine) {
          String part = token.substring(0, maxLine);

          if (inCodeBlock) {
            chunks.add(String.format("```\n%s```\n", part));
          } else {
            chunks.add(part);
          }

          token = token.substring(maxLine);
        }
        if (token.length() > 0) {
          codeBlockSize += token.length();
          chunk.append(token);
        }
      }
    }

    if (chunk.length() > 0) {
      if (inCodeBlock) {
        if (codeBlockSize == 0) {
          chunk.setLength(chunk.length() - "```\n".length());
        } else {
          chunk.append("```\n");
        }
      }
      if (chunk.length() > 0) {
        chunks.add(chunk.toString());
        chunk.setLength(0);
      }
    }

    return chunks;
  }
}
