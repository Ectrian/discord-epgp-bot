package com.epgpbot.epgpbot.commands;

import java.util.List;
import java.util.Optional;

import com.epgpbot.armory.transport.ArmoryRequest;
import com.epgpbot.armory.transport.ArmoryResponse;
import com.epgpbot.config.ArmoryType;
import com.epgpbot.database.Cursor;
import com.epgpbot.database.Statement;
import com.epgpbot.database.Transaction;
import com.epgpbot.epgpbot.schema.PermissionType;
import com.epgpbot.transport.CommandContext;
import com.epgpbot.transport.Request;
import com.epgpbot.util.PlayerId;
import com.google.common.collect.ImmutableList;

public class PlayerCharacterAddCommandHandler extends CommandHandlerAbstract {
  // TODO: Audit logging.
  public void handleAddCharacters(CommandContext context, Request request) throws Exception {
    try (Transaction tx = context.database().transaction()) {
      PlayerId player = getPlayerId(tx, request.arguments().get(0));
      if (player == null) {
        sendError(context, "Unknown player '%s'.", request.arguments().get(0));
        return;
      }

      for (String character : request.argumentsFrom(1)) {
        try (Statement q =
            tx.prepare("SELECT id, player_id FROM characters WHERE lower(name) = :name;")) {
          q.bind("name", character.toLowerCase());
          try (Cursor r = q.executeFetch()) {
            if (!r.next()) {
              if (!request.hasFlag("no-validate") &&
                  context.config().game_armory_type != ArmoryType.NONE) {
                sendError(context, "Unknown character '%s'.", ucfirst(character));
                return;
              }

              try (Statement u = tx.prepare(
                  "INSERT INTO characters (name, player_id) VALUES (:name, :player_id);")) {
                u.bind("name", ucfirst(character));
                u.bind("player_id", player.id);
                u.executeUpdate();
              }
              continue;
            }

            long characterId = r.get("id", Long.class);
            Optional<Long> existingPlayerId = r.getNullable("player_id", Long.class);

            if (existingPlayerId.isPresent() && existingPlayerId.get() != player.id
                && !request.hasFlag("force")) {
              // TODO: Include existing player name in error message.
              sendError(context,
                  "Character '%s' belongs to another player - run with --force to replace.",
                  ucfirst(character));
              continue;
            }
            if (request.hasFlag("force")
                && !context.user().hasPermission(PermissionType.MODIFY_PERMISSIONS)) {
              sendError(context, "You do not have permission to use --force.");
              return;
            }

            try (Statement u =
                tx.prepare("UPDATE characters SET player_id = :player_id WHERE id = :id;")) {
              u.bind("id", characterId);
              u.bind("player_id", player.id);
              u.executeUpdate();
            }
          }
        }
      }
    }

    context.reply("Operation successful.");
  }

  @Override
  public void handle(CommandContext context, Request request) throws Exception {
    if (request.arguments().size() < 2) {
      sendCorrectUsage(context);
      return;
    }

    if (context.config().game_armory_type == ArmoryType.NONE || request.hasFlag("no-validate")) {
      // Skip armory validation.
      handleAddCharacters(context, request);
      return;
    }

    ArmoryRequest armoryRequest = new ArmoryRequest().addCharacters(request.argumentsFrom(1))
        .setCallback((ArmoryResponse rsp) -> {
          try {
            handleAddCharacters(context, request);
          } catch (Exception e) {
            e.printStackTrace(System.err);
          }
        });

    if (!context.armory().offer(armoryRequest)) {
      context.reply("I am too busy to service your request at this time - try again later.");
    }
  }

  @Override
  public String help() {
    return "<player:string> <...character:string> [--no-validate] [--force] - Links a character to a player.";
  }

  @Override
  public String command() {
    return "player.character.add";
  }

  @Override
  public List<PermissionType> permissions() {
    return ImmutableList.of(PermissionType.MODIFY_EPGP);
  }
}
