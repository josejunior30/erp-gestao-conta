package com.juneba.erp.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.juneba.erp.DTO.AccountLiteDto;
import com.juneba.erp.DTO.TransactionSummaryDto;
import com.juneba.erp.DTO.UserTransactionDto;
import com.juneba.erp.config.PluggyProperties;
import com.juneba.erp.util.DateTimeUtils;
import com.juneba.erp.util.formatPluggyResponse;
import com.juneba.erp.util.formatPluggyText;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class PluggyTransactionsHttpService {
	private static final Logger log = LoggerFactory.getLogger(PluggyTransactionsHttpService.class);

	private final OkHttpClient okHttpClient;
	private final PluggyAuthService pluggyAuthService;
	private final PluggyProperties pluggyProperties;
	private final ObjectMapper mapper = new ObjectMapper();

	public PluggyTransactionsHttpService(OkHttpClient okHttpClient, PluggyAuthService pluggyAuthService,
			PluggyProperties pluggyProperties) {
		this.okHttpClient = okHttpClient;
		this.pluggyAuthService = pluggyAuthService;
		this.pluggyProperties = pluggyProperties;
	}
	// Busca todas as transações de um item e devolve um JSON cru com a lista completa.
	public com.fasterxml.jackson.databind.node.ObjectNode fetchAllTransactionsByItemId(String itemId, LocalDate from,
			LocalDate to, String status, Integer pageSize) {

		ZoneId zone = ZoneId.of("America/Sao_Paulo");
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

		var out = mapper.createObjectNode();
		out.put("itemId", itemId);
		out.put("count", allTxs.size());
		out.set("transactions", allTxs);
		return out;
	}
	// Busca as transações e devolve um resumo (totais, ordenação, etc.).
	public TransactionSummaryDto fetchAllTransactionsByItemIdPretty(String itemId, LocalDate from, LocalDate to,
			String status, Integer pageSize) {

		if (itemId == null || itemId.isBlank())
			throw new IllegalArgumentException("itemId é obrigatório");

		ZoneId zone = ZoneId.of("America/Sao_Paulo");
		Instant fromInstant = (from != null) ? from.atStartOfDay(zone).toInstant() : null;
		Instant toInstant = (to != null) ? to.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant() : null;

		int size = (pageSize == null || pageSize < 1) ? 500 : Math.min(pageSize, 500);
		String apiKey = pluggyAuthService.getApiKey();

		ArrayNode accountsJson = listAllAccountsByItem(itemId, size, apiKey);
		Map<String, AccountLiteDto> accounts = toAccountMap(accountsJson);

		List<UserTransactionDto> txDtos = new ArrayList<>();
		for (String accountId : accounts.keySet()) {
			ArrayNode txs = listAllTransactionsByAccount(accountId, fromInstant, toInstant, status, size, apiKey);
			for (JsonNode t : txs) {
				AccountLiteDto acc = accounts.get(accountId);
				UserTransactionDto dto = toUserTransactionDto(t, acc, zone);
				txDtos.add(dto);
			}
		}

		BigDecimal totalInflow = txDtos.stream().map(UserTransactionDto::amount).filter(Objects::nonNull)
				.filter(a -> a.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalOutflow = txDtos.stream().map(UserTransactionDto::amount).filter(Objects::nonNull)
				.filter(a -> a.signum() < 0).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal net = txDtos.stream().map(UserTransactionDto::amount).filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);


		List<UserTransactionDto> ordered = txDtos.stream().sorted(Comparator
				.comparing(UserTransactionDto::dateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
				.thenComparing(u -> u.amount() == null ? BigDecimal.ZERO : u.amount().abs(), Comparator.reverseOrder()))
				.toList();

		return new TransactionSummaryDto(itemId, from, to, ordered.size(), totalInflow, totalOutflow, net,
				ordered);
	}
	// Lista todas as contas de um item, página por página, e junta tudo.
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
	// Lista todas as transações de uma conta, respeitando filtros e paginação.
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
	// Executa a chamada HTTP e retorna o array do JSON (ou vazio); trata erros.
	private ArrayNode executeArray(Request req, String primaryArrayField, String fallbackArrayField) {
		long t0 = System.nanoTime();
		try (Response resp = okHttpClient.newCall(req).execute()) {
			long ms = (System.nanoTime() - t0) / 1_000_000;
			if (!resp.isSuccessful()) {
				String body = (resp.body() != null) ? formatPluggyText.safeTrim(resp.body().string(), 512) : "";
				// Por quê: logar corpo curto para troubleshooting sem poluir logs
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
	// Converte o JSON de contas para um mapa de DTOs com dados básicos.
	private Map<String, AccountLiteDto> toAccountMap(ArrayNode accountsJson) {
		Map<String, AccountLiteDto> map = new HashMap<>();
		for (JsonNode a : accountsJson) {
			String id = a.path("id").asText(null);
			if (id == null)
				continue;
			String name = formatPluggyText.firstNonEmpty(a.path("name").asText(null), a.path("number").asText(null),
					"Conta");
			String type = a.path("type").asText(null);
			String last4 = formatPluggyText.last4(a.path("number").asText(null), a.path("mask").asText(null));
			map.put(id, new AccountLiteDto(id, name, type, last4));
		}
		return map;
	}
	
	// Transforma um JSON de transação em um DTO amigável para a aplicação.
	private UserTransactionDto toUserTransactionDto(JsonNode t, AccountLiteDto acc, ZoneId zone) {
		String id = t.path("id").asText(null);
		Instant sortKey = DateTimeUtils.extractInstant(t, zone);
		LocalDateTime dateTime = (sortKey != null) ? LocalDateTime.ofInstant(sortKey, zone) : null;

		String description = formatPluggyText.firstNonEmpty(t.path("description").asText(null),
				t.path("descriptionRaw").asText(null), t.path("paymentData").path("description").asText(null),
				"Transação");
		String merchantName = formatPluggyText.firstNonEmpty(t.path("merchant").path("name").asText(null),
				t.path("merchant").asText(null), description);
		String category = formatPluggyText.firstNonEmpty(t.path("category").path("primary").asText(null),
				t.path("category").asText(null), t.path("categoryId").asText(null), "Outros");

		BigDecimal amount = formatPluggyResponse.safeBigDecimal(t.path("amount"));
		String currency = formatPluggyText.firstNonEmpty(t.path("currencyCode").asText(null),
				t.path("currency").asText(null), (acc != null ? "BRL" : null));
		String amountFormatted = formatPluggyResponse.formatCurrency(amount, currency);

		String type = (amount != null && amount.signum() < 0) ? "outflow" : "inflow";
		String status = formatPluggyText.firstNonEmpty(t.path("status").asText(null), "POSTED");

		return new UserTransactionDto(id, dateTime, description, merchantName, category, type, amount, amountFormatted,
				currency, status, acc);
	}
	
	
}