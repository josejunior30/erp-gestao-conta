package com.juneba.erp.service;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juneba.erp.DTO.ConnectorDto;
import com.juneba.erp.DTO.ItemDetailsDto;
import com.juneba.erp.config.PluggyProperties;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Service
public class PluggyItemService {
  private static final Logger log = LoggerFactory.getLogger(PluggyItemService.class); 

  private final OkHttpClient okHttpClient;
  private final PluggyAuthService pluggyAuthService;
  private final PluggyProperties pluggyProperties;

  private ObjectMapper mapper = new ObjectMapper();

  public PluggyItemService(OkHttpClient okHttpClient, PluggyAuthService pluggyAuthService, PluggyProperties pluggyProperties) {
    this.okHttpClient = okHttpClient;
    this.pluggyAuthService =pluggyAuthService;
    this.pluggyProperties = pluggyProperties;
  }

  public ItemDetailsDto fetchItemDetails(String itemId) {
    String apiKey =pluggyAuthService.getApiKey();
    HttpUrl url = HttpUrl.parse(pluggyProperties.getBaseUrl() + "/items/" + itemId).newBuilder().build();
    Request req = new Request.Builder().url(url).get().header("X-API-KEY", apiKey).build();

    long t0 = System.nanoTime();
    try (okhttp3.Response resp = okHttpClient.newCall(req).execute()) {
      long ms = (System.nanoTime() - t0) / 1_000_000;
      if (!resp.isSuccessful()) {
        String body = resp.body() != null ? safeTrim(resp.body().string(), 512) : "";
        log.warn("HTTP {} {} ({} ms) body='{}'", resp.code(), req.url(), ms, body);
        throw new IllegalStateException("Falha HTTP " + resp.code() + " em " + req.url());
      }

      String raw = Objects.requireNonNull(resp.body()).string();
      JsonNode json = mapper.readTree(raw);

      String id = json.path("id").asText(null);

      JsonNode c = json.path("connector");
      String connId = c.path("id").isNumber() ? String.valueOf(c.path("id").asLong()) : c.path("id").asText(null);
      String name = c.path("name").asText(null);
      String primaryColor = c.path("primaryColor").asText(null);
      String institutionUrl = firstNonEmpty(
          c.path("institutionUrl").asText(null),
          c.path("imageUrl").asText(null),
          c.path("logoUrl").asText(null)
      );
      String country = c.path("country").asText(null);
      String type = c.path("type").asText(null);

      ConnectorDto connector = new ConnectorDto(connId, name, primaryColor, institutionUrl, country, type);
      return new ItemDetailsDto(id, connector);

    } catch (IOException e) {
      log.error("Erro I/O em {}: {}", req.url(), e.getMessage(), e);
      throw new RuntimeException("Erro de I/O ao obter item", e);
    }
  }

  private static String safeTrim(String s, int max) {
    if (s == null) return "null";
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }

  private static String firstNonEmpty(String... vals) {
    if (vals == null) return null;
    for (String v : vals) if (v != null && !v.isBlank()) return v;
    return null;
  }
}