package com.epgpbot.util;

import java.util.ArrayList;
import java.util.List;

// Parses EPGP bot commands.
// Commands are in UNIX bash-like format...
// e.g. !command arg1 arg2 "quoted arg" --flag --flag with-value
public class CommandParser {
  enum State {
    BEGIN_TOKEN, IN_LITERAL, IN_DQUOTED_LITERAL, IN_SQUOTED_LITERAL, TOKEN_GAP,
  }

  public static class ParseException extends Exception {
    private static final long serialVersionUID = 1L;

    public ParseException(String error) {
      super(error);
    }
  }

  public static List<String> tokenize(String input) throws Exception {
    List<String> tokens = new ArrayList<String>();
    State state = State.BEGIN_TOKEN;
    String token = null;
    int i = 0;

    parse: while (true) {
      switch (state) {
        case BEGIN_TOKEN: {
          token = "";

          if (i >= input.length()) {
            break parse;
          }

          switch (input.charAt(i)) {
            case '"':
              state = State.IN_DQUOTED_LITERAL;
              i++;
              continue;
            case '\'':
              state = State.IN_SQUOTED_LITERAL;
              i++;
              continue;
            case ' ':
            case '\n':
            case '\t':
              i++;
              continue;
            default:
              state = State.IN_LITERAL;
              continue;
          }
        }

        case TOKEN_GAP: {
          if (i >= input.length()) {
            break parse;
          }

          switch (input.charAt(i)) {
            case ' ':
            case '\n':
            case '\t':
              state = State.BEGIN_TOKEN;
              i++;
              continue;
            default:
              throw new ParseException("Expected gap between literals.");
          }
        }

        case IN_LITERAL: {
          if (i >= input.length()) {
            tokens.add(token);
            state = State.TOKEN_GAP;
            continue;
          }

          switch (input.charAt(i)) {
            case ' ':
            case '\n':
            case '\t':
              tokens.add(token);
              state = State.TOKEN_GAP;
              continue;
            default:
              token += input.charAt(i);
              i++;
              continue;
          }
        }

        case IN_DQUOTED_LITERAL: {
          if (i >= input.length()) {
            throw new ParseException("Unterminated double-quoted literal.");
          }

          switch (input.charAt(i)) {
            case '"':
              tokens.add(token);
              state = State.TOKEN_GAP;
              i++;
              continue;
            default:
              token += input.charAt(i);
              i++;
              continue;
          }
        }

        case IN_SQUOTED_LITERAL: {
          if (i >= input.length()) {
            throw new ParseException("Unterminated single-quoted literal.");
          }

          switch (input.charAt(i)) {
            case '\'':
              tokens.add(token);
              state = State.TOKEN_GAP;
              i++;
              continue;
            default:
              token += input.charAt(i);
              i++;
              continue;
          }
        }
      }
    }

    return tokens;
  }
}
