package com.epgpbot.armory.transport;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public interface ArmoryAPI {
  public static class ArmoryAPIResponse {
    public String error;
  }

  public static class Guild extends ArmoryAPIResponse {
    List<Character> roster;

    @Override
    public String toString() {
      return String.format("Guild<roster=%s>", roster);
    }
  }

  public static class Character extends ArmoryAPIResponse {
    public static class Talents {
      String tree;
      List<Integer> points;

      @Override
      public String toString() {
        return String.format("Talents<tree=%s,points=%s>", tree, points);
      }
    }

    public String name;
    public String level;
    public String race;
    public @SerializedName("class") String clazz; // "class" is reserved keyword
    public String guild;
    public List<Talents> talents;
    public boolean online;

    @Override
    public String toString() {
      return String.format("Character<name=%s, level=%s, race=%s, class=%s, guild=%s, talents=%s>",
          name, level, race, clazz, guild, talents);
    }
  }

  public Guild getGuild(String realm, String guild) throws Exception;

  public Character getCharacter(String realm, String character) throws Exception;

  public Guild getGuild(String guild) throws Exception;

  public Character getCharacter(String character) throws Exception;
}
