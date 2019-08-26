package com.epgpbot.epgpbot.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.epgpbot.schema.game.CharacterClass;
import com.epgpbot.epgpbot.schema.game.CharacterRace;
import com.epgpbot.epgpbot.schema.game.CharacterSpec;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class CharacterUpdateCommandHandler extends CommandHandlerAbstract {
  // TODO: Way more validation.
  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() != 1) {
      sendCorrectUsage(context);
      return;
    }

    String characterName = request.arguments().get(0);

    Map<String, Object> props = new HashMap<>();

    if (request.hasFlag("level")) {
      if (request.flag("level").size() != 1) {
        sendCorrectUsage(context);
        return;
      }
      try {
        int level = Integer.parseInt(request.flag("level").get(0));
        if (level < 1 || level > context.config().game_expansion.maxCharacterLevel) {
          sendCorrectUsage(context);
          return;
        }
        props.put("level", level);
      } catch (NumberFormatException e) {
        sendCorrectUsage(context);
        return;
      }
    }

    if (request.hasFlag("race")) {
      if (request.flag("race").size() != 1) {
        sendCorrectUsage(context);
        return;
      }
      for (CharacterRace r : CharacterRace.values()) {
        if (r.name.toLowerCase().equals(request.flag("race").get(0).toLowerCase())) {
          props.put("race", r.name);
          break;
        }
      }
    }

    if (request.hasFlag("class")) {
      if (request.flag("class").size() != 1) {
        sendCorrectUsage(context);
        return;
      }
      for (CharacterClass r : CharacterClass.values()) {
        if (r.name.toLowerCase().equals(request.flag("class").get(0).toLowerCase())) {
          props.put("class", r.name);
          break;
        }
      }
    }

    if (request.hasFlag("guild")) {
      if (request.flag("guild").size() != 1) {
        sendCorrectUsage(context);
        return;
      }
      props.put("guild_name", request.flag("guild").get(0));
    }

    if (request.hasFlag("spec1")) {
      if (request.flag("spec1").size() != 1) {
        sendCorrectUsage(context);
        return;
      }
      for (CharacterSpec r : CharacterSpec.values()) {
        if (r.name.toLowerCase().equals(request.flag("spec1").get(0).toLowerCase())) {
          props.put("talent_spec1_tree", r.name);
          break;
        }
      }
    }

    if (request.hasFlag("spec2")) {
      if (request.flag("spec2").size() != 1) {
        sendCorrectUsage(context);
        return;
      }
      for (CharacterSpec r : CharacterSpec.values()) {
        if (r.name.toLowerCase().equals(request.flag("spec2").get(0).toLowerCase())) {
          props.put("talent_spec2_tree", r.name);
          break;
        }
      }
    }

    if (request.hasFlag("level")) {

    }

    if (props.size() == 0) {
      sendCorrectUsage(context);
      return;
    }

    try (Transaction tx = context.database().transaction()) {
      try (Statement q = tx.prepare("SELECT id FROM characters WHERE lower(name) = :name;")) {
        q.bind("name", characterName.toLowerCase());
        try (Cursor r = q.executeFetch()) {
          if (!r.next()) {
            try (Statement u = tx.prepare("INSERT INTO characters (name) VALUES (:name);")) {
              u.bind("name", ucfirst(characterName));
              u.executeUpdate();
            }
          }
        }
      }

      String fields = Joiner.on(", ").join(props.keySet().stream().map((String key) -> String.format("%s = :%s", key, key)).iterator());
      try (Statement q = tx.prepare("UPDATE characters SET " + fields + " WHERE lower(name) = :name;")) {
        for (Map.Entry<String, Object> entry : props.entrySet()) {
          q.bind(entry.getKey(), entry.getValue());
        }
        q.bind("name", characterName.toLowerCase());
        q.executeUpdate();
      }
    }

    context.replyf("Operation successful.");
  }

  @Override
  public String help() {
    return "<name:string> [--level <int> --race <string> --class <string> --guild <string> --spec1 <string> --spec2 <string>] - Updates character information manually.";
  }

  @Override
  public String command() {
    return "character.update";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
