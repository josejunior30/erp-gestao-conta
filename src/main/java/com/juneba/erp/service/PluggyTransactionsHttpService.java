package com.juneba.erp.service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.juneba.erp.config.PluggyProperties;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

@Service
public class PluggyTransactionsHttpService {
	private static final Logger log = LoggerFactory.getLogger(PluggyTransactionsHttpService.class); 

	private final OkHttpClient okHttpClient;
	private final PluggyAuthService  pluggyAuthService ;
	private final PluggyProperties pluggyProperties;

	private ObjectMapper mapper = new ObjectMapper();



	public PluggyTransactionsHttpService(OkHttpClient okHttpClient,
			PluggyAuthService pluggyAuthService,
			PluggyProperties pluggyProperties) {
		super();
		this.okHttpClient = okHttpClient;
		this.pluggyAuthService = pluggyAuthService;
		this.pluggyProperties = pluggyProperties;
	}

	/** Agrega transações de todas as contas de um item. */
	public ObjectNode fetchAllTransactionsByItemId(String itemId, LocalDate from, LocalDate to, String status,
			Integer pageSize) {
		ZoneId zone = ZoneId.of("America/Sao_Paulo"); // fuso padrão BR
		Instant fromInstant = (from != null) ? from.atStartOfDay(zone).toInstant() : null;
		Instant toInstant = (to != null) ? to.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant() : null;

		int size = (pageSize == null || pageSize < 1) ? 500 : Math.min(pageSize, 500);
		String apiKey = pluggyAuthService.getApiKey();

		ArrayNode accounts = listAllAccountsByItem(itemId, size, apiKey);
		ArrayNode allTxs = mapper.createArrayNode();

		for (JsonNode acc : accounts) {
			String accountId = acc.path("id").asText(null);
			if (accountId == null || accountId.isBlank())
				continue;
			ArrayNode txs = listAllTransactionsByAccount(accountId, fromInstant, toInstant, status, size, apiKey);
			allTxs.addAll(txs);
		}

		ObjectNode out = mapper.createObjectNode();
		out.put("itemId", itemId);
		out.put("count", allTxs.size());
		out.set("transactions", allTxs);
		return out;
	}

	private ArrayNode listAllAccountsByItem(String itemId, int pageSize, String apiKey) {
		ArrayNode accs = mapper.createArrayNode();
		int page = 1;
		while (true) {
			HttpUrl url = HttpUrl.parse(pluggyProperties.getBaseUrl() + "/accounts").newBuilder()
					.addQueryParameter("itemId", itemId).addQueryParameter("page", String.valueOf(page))
					.addQueryParameter("pageSize", String.valueOf(pageSize)).build();

			Request req = new Request.Builder().url(url).get().header("X-API-KEY", apiKey).build();

			ArrayNode pageResults = executeArray(req, "results", "accounts");
			if (pageResults.isEmpty())
				break;

			accs.addAll(pageResults);
			if (pageResults.size() < pageSize)
				break;
			page++;
		}
		return accs;
	}

	private ArrayNode listAllTransactionsByAccount(String accountId, Instant from, Instant to, String status,
			int pageSize, String apiKey) {
		ArrayNode txs = mapper.createArrayNode();
		int page = 1;
		while (true) {
			HttpUrl.Builder b = HttpUrl.parse(pluggyProperties.getBaseUrl() + "/transactions").newBuilder()
					.addQueryParameter("accountId", accountId).addQueryParameter("page", String.valueOf(page))
					.addQueryParameter("pageSize", String.valueOf(pageSize));

			if (from != null)
				b.addQueryParameter("from", from.toString());
			if (to != null)
				b.addQueryParameter("to", to.toString());
			if (status != null && !status.isBlank())
				b.addQueryParameter("status", status);

			Request req = new Request.Builder().url(b.build()).get().header("X-API-KEY", apiKey).build();

			ArrayNode pageResults = executeArray(req, "results", "transactions");
			if (pageResults.isEmpty())
				break;

			txs.addAll(pageResults);
			if (pageResults.size() < pageSize)
				break;
			page++;
		}
		return txs;
	}


	private ArrayNode executeArray(Request req, String primaryArrayField, String fallbackArrayField) {
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

			JsonNode arr = json.path(primaryArrayField);
			if (!arr.isArray() || arr.size() == 0)
				arr = json.path(fallbackArrayField);
			return (arr.isArray()) ? (ArrayNode) arr : mapper.createArrayNode();
		} catch (IOException e) {
			log.error("Erro I/O em {}: {}", req.url(), e.getMessage(), e);
			throw new RuntimeException("Erro de I/O em chamada Pluggy", e);
		}
	}

	private static String safeTrim(String s, int max) {
		if (s == null)
			return "null";
		return s.length() <= max ? s : s.substring(0, max) + "…";
	}
}
