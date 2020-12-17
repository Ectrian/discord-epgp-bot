package com.epgpbot.epgpbot.commands;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.EPGPEventType;
import com.epgpbot.epgpbot.schema.LootInfo;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.Argument;
import com.epgpbot.util.PlayerId;
import com.google.common.collect.ImmutableList;

public class EquipCommandHandler extends CommandHandlerAbstract {
  enum State {
    BEGIN_TOKEN, IN_LITERAL,
  }

  private List<String> parseConcatedLinkNames(String input) throws Exception {
    List<String> tokens = new ArrayList<String>();
    State state = State.BEGIN_TOKEN;
    String token = null;
    int i = 0;

    parse: while (true) {
      switch (state) {
        case BEGIN_TOKEN: {

          if (i >= input.length()) {
            break parse;
          }

          switch (input.charAt(i)) {
            case '[':
              state = State.IN_LITERAL;
              token = "";
              i++;
              continue;
            case ' ':
            case '\r':
            case '\n':
            case '\t':
              i++;
              continue;
            default:
              throw new Exception("Parse failure: expected '['");
          }
        }

        case IN_LITERAL: {
          if (i >= input.length()) {
            throw new Exception("Parse failure: unexpected end-of-string");
          }

          switch (input.charAt(i)) {
            case ']':
              tokens.add(token);
              state = State.BEGIN_TOKEN;
              i++;
              continue;
            case '[':
              throw new Exception("Unexpected nexted '['");
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

  private List<LootInfo> parseConcatedLinks(Transaction tx, String input) throws Exception {
    List<LootInfo> out = new ArrayList<>();
    for (String item : parseConcatedLinkNames(input)) {
      Argument arg = new Argument("item", item);
      out.add(arg.lootValue(tx));
    }
    return out;
  }

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    try (Transaction tx = context.database().transaction()) {
      PlayerId player = request.arg("character", 0).characterPlayerIdValue(tx);
      List<LootInfo> items = new ArrayList<>();

      for (Argument arg : request.argsFrom("item", 1)) {
        if (arg.stringValue().startsWith("[")) {
          for (LootInfo item : parseConcatedLinks(tx, arg.stringValue())) {
            items.add(item);
          }
        }
        else {
          items.add(arg.lootValue(tx));
        }
      }

      if (!context.user().hasPlayer()) {
        sendError(context, "You must link your Discord account to a player record first.");
        return;
      }

      for (LootInfo item : items) {
        try (Statement q = tx.prepare(
            "INSERT INTO epgp_log (",
            "  timestamp, ",
            "  action_timestamp, ",
            "  target_player_id, ",
            "  target_character_id, ",
            "  source_player_id, ",
            "  type, ",
            "  loot_id, ",
            "  ep_delta, ",
            "  gp_delta",
            ") VALUES (",
            "  :timestamp, ",
            "  :timestamp, ",
            "  :target_player_id, ",
            "  :target_character_id, ",
            "  :source_player_id, ",
            "  " + EPGPEventType.EQUIP.ordinal() +", ",
            "  :loot_id, ",
            "  0, ",
            "  0",
            ");"
        )) {
          q.bind("timestamp", Instant.now().getEpochSecond());
          q.bind("target_player_id", player.id);
          q.bind("target_character_id", player.characterId);
          q.bind("source_player_id", context.user().playerId());
          q.bind("loot_id", item.lootId);
          q.executeInsert();
        }
      }
    }

    context.reply("Operation successful.");
  }

  @Override
  public String help() {
    return "<character:string> <...item:string> - Equips item(s) to a character in the UI.";
  }

  @Override
  public String command() {
    return "equip";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }

}
