package com.juneba.erp.service;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juneba.erp.config.PluggyProperties;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class PluggyAuthService {

	private static final Logger log = LoggerFactory.getLogger(PluggyAuthService.class);
	private final OkHttpClient http;
	private final PluggyProperties props;
	private final ObjectMapper mapper = new ObjectMapper();
	private final AtomicReference<String> cachedApiKey = new AtomicReference<>();
	private volatile Instant apiKeyExpiresAt = Instant.EPOCH;

	public PluggyAuthService(OkHttpClient http, PluggyProperties props) {
		this.http = http;
		this.props = props;
	}

	//solicita apiKey e retorna a chave para autorizar chamadas subsequentes.
	public String getApiKey() {
		final Instant now = Instant.now();
		final String cached = cachedApiKey.get();
		if (cached != null && now.isBefore(apiKeyExpiresAt)) {
			log.debug("PluggyAuthService.getApiKey: usando cache; expira em {}",
					DateTimeFormatter.ISO_INSTANT.format(apiKeyExpiresAt));
			return cached;
		}

		final String url = props.getBaseUrl() + "/auth";
		final RequestBody body = RequestBody.create("""
				{"clientId":"%s","clientSecret":"%s"}
				""".formatted(props.getClientId(), props.getClientSecret()), MediaType.parse("application/json"));

		final Request req = new Request.Builder().url(url).post(body).build();

		log.debug("POST {} (obter apiKey)", url);
		final long t0 = System.nanoTime();

		try (Response resp = http.newCall(req).execute()) {
			final long ms = (System.nanoTime() - t0) / 1_000_000;
			if (!resp.isSuccessful()) {
				final String errBody = resp.body() != null ? safeTrim(resp.body().string(), 512) : "";
				log.warn("Falha /auth: HTTP {} em {} ms; body='{}'", resp.code(), ms, errBody);
				throw new IllegalStateException("Falha ao autenticar na Pluggy: HTTP " + resp.code());
			}

			final String raw = Objects.requireNonNull(resp.body()).string();
			final JsonNode json = mapper.readTree(raw);
			final String apiKey = json.path("apiKey").asText(null);

			if (apiKey == null || apiKey.isBlank()) {
				log.error("Resposta /auth sem apiKey; body='{}'", safeTrim(raw, 512));
				throw new IllegalStateException("Resposta sem apiKey do /auth");
			}

			cachedApiKey.set(apiKey);
			apiKeyExpiresAt = Instant.now().plusSeconds(110 * 60L);

			log.info("apiKey obtida em {} ms; cache até {}", ms, DateTimeFormatter.ISO_INSTANT.format(apiKeyExpiresAt));
			return apiKey;

		} catch (IOException e) {
			log.error("Erro I/O em /auth: {}", e.getMessage(), e);
			throw new RuntimeException("Erro de I/O no /auth", e);
		}
	}

	private static String safeTrim(String s, int max) {
		if (s == null)
			return "null";
		return s.length() <= max ? s : s.substring(0, max) + "…";
	}
}