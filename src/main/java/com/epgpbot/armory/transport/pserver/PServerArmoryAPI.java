package com.epgpbot.armory.transport.pserver;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.epgpbot.armory.transport.ArmoryAPI;
import com.epgpbot.config.Config;
import com.google.common.collect.ImmutableMap;

/**
 * Caution: API is very, very janky. Validate EVERYTHING!
 */
public class PServerArmoryAPI extends RestAPI implements ArmoryAPI {
  private final Config config;

  public PServerArmoryAPI(Config config) {
    this.config = config;
  }

  @Override
  protected String buildFullUrl(String path, Map<String, String> parameters)
      throws UnsupportedEncodingException {
    return String.format("http://%s/api/%s?%s", config.game_armory_url, path,
        urlencode(parameters));
  }

  protected <T extends ArmoryAPIResponse> T getWithRetries(Class<T> type, String path,
      Map<String, String> parameters, Map<String, String> headers, int attempts) throws Exception {
    assert attempts > 0;
    String lastError = null;

    for (int i = 0; i < attempts; i++) {
      T data = get(type, path, parameters, headers);

      // Instead of returning HTTP errors, API gives 200s with "error" property.
      if (data.error != null) {
        // API server throttles to one request at a time... somewhat imperfectly.
        // Just wait and re-try.
        if (data.error.contains("Too many requests")) {
          Thread.sleep(1000);
          continue;
        }
        if (data.error.contains("does not exist")) {
          return null;
        }
        break;
      }

      return data;
    }

    throw new Exception(String.format("API request failed: %s", lastError));
  }

  @Override
  public Guild getGuild(String realm, String guild) throws Exception {
    return getWithRetries(Guild.class, String.format("guild/%s/%s/summary", guild, realm),
        ImmutableMap.of(), ImmutableMap.of(), 10);
  }

  @Override
  public Character getCharacter(String realm, String character) throws Exception {
    return getWithRetries(Character.class,
        String.format("character/%s/%s/summary", character, realm), ImmutableMap.of(),
        ImmutableMap.of(), 10);
  }

  @Override
  public Guild getGuild(String guild) throws Exception {
    return getGuild(config.game_realm_name, guild);
  }

  @Override
  public Character getCharacter(String character) throws Exception {
    return getCharacter(config.game_realm_name, character);
  }
}
