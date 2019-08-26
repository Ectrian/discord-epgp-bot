package com.epgpbot.armory.transport.pserver;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epgpbot.util.JsonParser;
import com.google.common.base.Joiner;

public abstract class RestAPI {
  public RestAPI() {

  }

  protected void addStandardHeaders(Map<String, String> headers) {
    headers.put("User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.80 Safari/537.36");
  }

  protected String urlencode(Map<String, String> headers) throws UnsupportedEncodingException {
    List<String> parts = new ArrayList<>();

    for (Map.Entry<String, String> entry : headers.entrySet()) {
      parts.add(String.format("%s=%s",
          URLEncoder.encode(entry.getKey(), Charset.defaultCharset().toString()),
          URLEncoder.encode(entry.getValue(), Charset.defaultCharset().toString())));
    }

    return Joiner.on("&").join(parts);
  }

  protected <T> T get(Class<T> type, String path, Map<String, String> parameters,
      Map<String, String> headers) throws Exception {
    String fullUrl = buildFullUrl(path, parameters);
    System.out.format("GET %s\n", fullUrl);
    headers = new HashMap<>(headers);
    addStandardHeaders(headers);
    URL obj = new URL(fullUrl);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    con.setRequestMethod("GET");

    for (Map.Entry<String, String> header : headers.entrySet()) {
      con.setRequestProperty(header.getKey(), header.getValue());
    }

    int responseCode = con.getResponseCode();
    if (responseCode != 200) {
      throw new Exception("API request failed; rc=" + responseCode);
    }

    return JsonParser.newJsonParser().fromJson(new InputStreamReader(con.getInputStream()), type);
  }

  protected abstract String buildFullUrl(String path, Map<String, String> parameters)
      throws UnsupportedEncodingException;
}
