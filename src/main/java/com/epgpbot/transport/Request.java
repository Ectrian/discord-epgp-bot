package com.epgpbot.transport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epgpbot.util.Argument;
import com.epgpbot.util.CommandParser;
import com.google.common.collect.ImmutableList;

public class Request {
  private final String message;
  private final String command;
  private final List<String> arguments;
  private final Map<String, List<String>> flags;
  private final List<User> mentions;

  public Request(String message, List<User> mentions) throws Exception {
    List<String> tokens = CommandParser.tokenize(message);

    // We know message must be non-empty, starting with !.
    this.message = message;
    this.mentions = mentions;
    this.command = tokens.get(0).substring(1);
    this.arguments = new ArrayList<>();
    this.flags = new HashMap<>();

    String curFlagName = null;
    List<String> curFlag = null;

    // Take tokenized command and extract arguments and flags.
    // e.g. !command a b "c d" --flag1 --flag2 value1
    // yields arguments = ["a", "b", "c d"]
    // flags = {"flag1": [], "flag2": ["value1"]}
    for (int i = 1; i < tokens.size(); i++) {
      String token = tokens.get(i);

      if (token.startsWith("--")) {
        if (curFlagName != null) {
          this.flags.put(curFlagName, curFlag);
        }

        curFlagName = token.substring(2);
        curFlag = new ArrayList<>();
      } else {
        if (curFlagName != null) {
          curFlag.add(token);
        } else {
          arguments.add(token);
        }
      }
    }

    if (curFlagName != null) {
      this.flags.put(curFlagName, curFlag);
    }
  }

  public boolean hasFlag(String name) {
    return this.flags.containsKey(name);
  }

  public List<String> flag(String name) {
    return this.flags.getOrDefault(name, ImmutableList.of());
  }

  // Any @mentions contained in the message.
  public List<User> mentions() {
    return this.mentions;
  }

  public String message() {
    return this.message;
  }

  public String command() {
    return this.command;
  }

  public List<String> arguments() {
    return this.arguments;
  }

  public List<String> argumentsFrom(int index) {
    return new ArrayList<>(arguments()).subList(index, arguments().size());
  }

  public Map<String, List<String>> flags() {
    return this.flags;
  }

  public static Argument arg(String name, String value) {
    return new Argument(name, value);
  }

  public static Argument arg(String value) {
    return arg(null, value);
  }

  public static List<Argument> args(String name, List<String> values) {
    List<Argument> args = new ArrayList<>();
    for (String value : values) {
      args.add(arg(name, value));
    }
    return args;
  }

  public static List<Argument> args(List<String> values) {
    return args(null, values);
  }

  public Argument arg(String name, int i) {
    if (i >= arguments().size()) {
      return arg(name, null);
    }
    return arg(name, arguments().get(i));
  }

  public List<Argument> argsFrom(String name, int i) {
    return args(name, argumentsFrom(i));
  }

  public List<Argument> args(String name) {
    return argsFrom(name, 0);
  }

  public Argument flagArg(String name) {
    if (!hasFlag(name) || flag(name).size() != 1) {
      return arg(name, null);
    }
    return arg(name, flag(name).get(0));
  }

  public List<Argument> flagArgs(String name) {
    if (!hasFlag(name) || flag(name).isEmpty()) {
      return ImmutableList.of();
    }
    return args(name, flag(name));
  }
}
