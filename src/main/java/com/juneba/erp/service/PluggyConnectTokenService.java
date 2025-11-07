package com.juneba.erp.service;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.juneba.erp.config.PluggyProperties;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class PluggyConnectTokenService {
	private static final Logger log = LoggerFactory.getLogger(PluggyConnectTokenService.class);

	private final OkHttpClient http;
	private final PluggyAuthService auth;
	private final PluggyProperties props;
	private final ObjectMapper mapper = new ObjectMapper();

	public PluggyConnectTokenService(OkHttpClient http, PluggyAuthService auth, PluggyProperties props) {
		this.http = http;
		this.auth = auth;
		this.props = props;
	}

//pega a apikey, para  connect token e para abrir o widget no front 
	public String createConnectToken(String clientUserId, boolean avoidDuplicates, String itemId,
			String oauthRedirectUri) {

		final ObjectNode options = mapper.createObjectNode();
		if (clientUserId != null && !clientUserId.isBlank())
			options.put("clientUserId", clientUserId);
		if (props.getWebhookUrl() != null && !props.getWebhookUrl().isBlank())
			options.put("webhookUrl", props.getWebhookUrl());
		if (oauthRedirectUri != null && !oauthRedirectUri.isBlank())
			options.put("oauthRedirectUri", oauthRedirectUri);
		options.put("avoidDuplicates", avoidDuplicates);

		final ObjectNode payload = mapper.createObjectNode();
		payload.set("options", options);
		if (itemId != null && !itemId.isBlank())
			payload.put("itemId", itemId);

		final String url = props.getBaseUrl() + "/connect_token";
		final RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));

		final String apiKey = auth.getApiKey();
		final Request req = new Request.Builder().url(url).post(body).header("X-API-KEY", apiKey).build();

		log.debug("POST {} (connect token); clientUserId='{}', avoidDuplicates={}, itemId='{}', hasOauthRedirect={}",
				url, safe(clientUserId), avoidDuplicates, safe(itemId),
				oauthRedirectUri != null && !oauthRedirectUri.isBlank());

		final long t0 = System.nanoTime();

		try (Response resp = http.newCall(req).execute()) {
			final long ms = (System.nanoTime() - t0) / 1_000_000;

			if (!resp.isSuccessful()) {
				final String errBody = resp.body() != null ? safeTrim(resp.body().string(), 512) : "";
				log.warn("Falha /connect_token: HTTP {} em {} ms; body='{}'", resp.code(), ms, errBody);
				throw new IllegalStateException("Falha ao criar connect token: HTTP " + resp.code());
			}

			final String raw = Objects.requireNonNull(resp.body()).string();
			final JsonNode json = mapper.readTree(raw);

			String token = json.path("connectToken").asText(null);
			if (token == null || token.isBlank())
				token = json.path("accessToken").asText(null);

			if (token == null || token.isBlank()) {
				log.error("Resposta /connect_token sem token; body='{}'", safeTrim(raw, 512));
				throw new IllegalStateException("Resposta sem connect token do /connect_token");
			}

			log.info("connectToken criado em {} ms;", ms);
			return token;

		} catch (IOException e) {
			log.error("Erro I/O em /connect_token: {}", e.getMessage(), e);
			throw new RuntimeException("Erro de I/O no /connect_token", e);
		}
	}

	private static String safe(String s) {
		return (s == null || s.isBlank()) ? "" : s;
	}

	private static String safeTrim(String s, int max) {
		if (s == null)
			return "null";
		return s.length() <= max ? s : s.substring(0, max) + "…";
	}
}