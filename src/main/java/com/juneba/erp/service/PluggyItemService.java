package com.juneba.erp.service;

import static com.juneba.erp.util.JsonNodeUtils.asBigDecimal;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juneba.erp.DTO.AccountBalanceDto;
import com.juneba.erp.DTO.ConnectorDto;
import com.juneba.erp.DTO.ItemDetailsDto;
import com.juneba.erp.Exception.UpstreamException;
import com.juneba.erp.Exception.UpstreamIoException;
import com.juneba.erp.config.PluggyProperties;
import com.juneba.erp.entities.PluggyItem;
import com.juneba.erp.repository.PluggyItemRepository;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.juneba.erp.util.formatPluggyText;

@Service
public class PluggyItemService {
	private static final Logger log = LoggerFactory.getLogger(PluggyItemService.class);

	private final OkHttpClient okHttpClient;
	private final PluggyAuthService pluggyAuthService;
	private final PluggyProperties pluggyProperties;
	private final PluggyItemRepository pluggyItemRepository;
	private final ObjectMapper mapper = new ObjectMapper();

	public PluggyItemService(OkHttpClient okHttpClient, PluggyAuthService pluggyAuthService,
			PluggyProperties pluggyProperties, PluggyItemRepository pluggyItemRepository) {
		this.okHttpClient = okHttpClient;
		this.pluggyAuthService = pluggyAuthService;
		this.pluggyProperties = pluggyProperties;
		this.pluggyItemRepository = pluggyItemRepository;
	}

	public ItemDetailsDto fetchItemDetails(String itemId) {
		if (itemId == null || itemId.isBlank()) {
			throw new IllegalArgumentException("itemId é obrigatório");
		}

		String apiKey = pluggyAuthService.getApiKey();

		HttpUrl url = HttpUrl.parse(pluggyProperties.getBaseUrl() + "/items/" + itemId).newBuilder().build();

		Request req = new Request.Builder().url(url).get().header("X-API-KEY", apiKey).build();

		long t0 = System.nanoTime();
		try (Response resp = okHttpClient.newCall(req).execute()) {
			long ms = (System.nanoTime() - t0) / 1_000_000;

			ResponseBody respBody = resp.body();
			String rawBody = (respBody != null) ? respBody.string() : "";

			if (!resp.isSuccessful()) {
				String snippet = formatPluggyText.safeTrim(rawBody, 512);
				log.warn("HTTP {} {} ({} ms) body='{}'", resp.code(), req.url(), ms, snippet);
				throw new UpstreamException(resp.code(), req.url().toString(), snippet);
			}

			JsonNode json = mapper.readTree(rawBody);

			String id = json.path("id").asText(null);

			JsonNode c = json.path("connector");
			String connId = c.path("id").isNumber() ? String.valueOf(c.path("id").asLong()) : c.path("id").asText(null);
			String name = c.path("name").asText(null);
			String primaryColor = c.path("primaryColor").asText(null);
			String institutionUrl = formatPluggyText.firstNonEmpty(c.path("institutionUrl").asText(null),
					c.path("imageUrl").asText(null), c.path("logoUrl").asText(null));
			String country = c.path("country").asText(null);
			String type = c.path("type").asText(null);

			ConnectorDto connector = new ConnectorDto(connId, name, primaryColor, institutionUrl, country, type);

			List<AccountBalanceDto> accounts = fetchAccountsByItemId(itemId, apiKey);

			return new ItemDetailsDto(id, connector, accounts);

		} catch (IOException e) {
			log.error("Erro I/O em {}: {}", req.url(), e.getMessage(), e);
			throw new UpstreamIoException(req.url().toString(), e);
		} catch (RuntimeException e) {
			log.error("Falha ao obter item {} em {}: {}", itemId, req.url(), e.getMessage());
			throw e;
		}
	}

	public List<ItemDetailsDto> listAllItemDetailsFromDb() {
		List<String> itemIds = pluggyItemRepository.findAll().stream().map(PluggyItem::getItemId)
				.filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct()
				.collect(Collectors.toList());

		return itemIds.stream().map(id -> {
			try {
				return fetchItemDetails(id);
			} catch (RuntimeException e) {
				log.warn("Falha ao obter detalhes do itemId {}: {}", id, e.getMessage());
				return null;
			}
		}).filter(Objects::nonNull).toList();
	}

	private List<AccountBalanceDto> fetchAccountsByItemId(String itemId, String apiKey) {
		HttpUrl url = HttpUrl.parse(pluggyProperties.getBaseUrl() + "/accounts").newBuilder()
				.addQueryParameter("itemId", itemId).addQueryParameter("pageSize", "200").build();

		Request req = new Request.Builder().url(url).get().header("X-API-KEY", apiKey).build();

		long t0 = System.nanoTime();
		try (Response resp = okHttpClient.newCall(req).execute()) {
			long ms = (System.nanoTime() - t0) / 1_000_000;
			ResponseBody body = resp.body();
			String raw = (body != null) ? body.string() : "";

			if (!resp.isSuccessful()) {
				log.warn("Falha ao buscar contas do item {} → HTTP {} {} ({} ms) body='{}'", itemId, resp.code(),
						req.url(), ms, formatPluggyText.safeTrim(raw, 512));
				return List.of();
			}

			JsonNode root = mapper.readTree(raw);
			JsonNode results = root.path("results");
			if (results == null || !results.isArray() || results.isEmpty()) {
				return List.of();
			}

			List<AccountBalanceDto> list = new ArrayList<>(results.size());
			for (JsonNode acc : results) {
				String accId = acc.path("id").isNumber() ? String.valueOf(acc.path("id").asLong())
						: acc.path("id").asText(null);

				String name = acc.path("name").asText(null);
				String type = acc.path("type").asText(null);
				String currencyCode = acc.path("currencyCode").asText(null);

				BigDecimal balance = asBigDecimal(acc, "balance");
				BigDecimal availableBalance = asBigDecimal(acc, "availableBalance");

				list.add(new AccountBalanceDto(accId, name, type, currencyCode, balance, availableBalance));
			}
			return list;

		} catch (IOException e) {
			log.warn("Erro I/O ao buscar contas do item {} em {}: {}", itemId, req.url(), e.getMessage());
			return List.of();
		} catch (RuntimeException e) {
			log.warn("Falha inesperada ao processar contas do item {}: {}", itemId, e.getMessage());
			return List.of();
		}
	}
}