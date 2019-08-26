package com.epgpbot.util;

import java.lang.reflect.Type;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class JsonParser {
  public static Gson newJsonParser() {
    return newJsonParserBuilder().create();
  }

  public static Gson newJsonParser(FieldNamingPolicy p) {
    return newJsonParserBuilder(p).create();
  }

  public static GsonBuilder newJsonParserBuilder() {
    return newJsonParserBuilder(FieldNamingPolicy.IDENTITY);
  }

  @SuppressWarnings("rawtypes")
  public static GsonBuilder newJsonParserBuilder(FieldNamingPolicy p) {
    return new GsonBuilder()
    .setPrettyPrinting()
    .setFieldNamingPolicy(p)
    .disableHtmlEscaping()
    .serializeNulls()
    .registerTypeHierarchyAdapter(Enum.class, new JsonSerializer<Enum>() {
      @Override
      public JsonElement serialize(Enum item, Type type, JsonSerializationContext ctx) {
        return ctx.serialize(item.ordinal());
      }
    })
    .registerTypeHierarchyAdapter(Enum.class, new JsonDeserializer<Enum>() {
      @Override
      public Enum deserialize(JsonElement item, Type type, JsonDeserializationContext ctx)
          throws JsonParseException {
        if (!(type instanceof Class)) {
          throw new JsonParseException("Unable to decode enum " + type.getTypeName());
        }

        Class enumType = (Class) type;
        Object[] values = enumType.getEnumConstants();

        try {
          int index = item.getAsInt();
          if (index >= 0 && index < values.length) {
            return (Enum) values[index];
          } else {
            throw new JsonParseException("Unable to decode enum " + type.getTypeName());
          }
        } catch (NumberFormatException e) {
        }

        String value = item.getAsString();
        for (int i = 0; i < values.length; i++) {
          if (values[i].toString().equals(value)) {
            return (Enum) values[i];
          }
        }

        throw new JsonParseException("Unable to decode enum " + type.getTypeName());
      }
    });
  }
}
