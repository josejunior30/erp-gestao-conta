package com.juneba.erp.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class PluggyItemService {
	private static final Logger log = LoggerFactory.getLogger(PluggyItemService.class);

	private final OkHttpClient okHttpClient;
	private final PluggyAuthService pluggyAuthService;
	private final PluggyProperties pluggyProperties;
	private final PluggyItemRepository pluggyItemRepository;
	private ObjectMapper mapper = new ObjectMapper();

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
				String snippet = safeTrim(rawBody, 512);
				log.warn("HTTP {} {} ({} ms) body='{}'", resp.code(), req.url(), ms, snippet);
				throw new UpstreamException(resp.code(), req.url().toString(), snippet);
			}

			JsonNode json = mapper.readTree(rawBody);

			String id = json.path("id").asText(null);

			JsonNode c = json.path("connector");
			String connId = c.path("id").isNumber() ? String.valueOf(c.path("id").asLong()) : c.path("id").asText(null);
			String name = c.path("name").asText(null);
			String primaryColor = c.path("primaryColor").asText(null);
			String institutionUrl = firstNonEmpty(c.path("institutionUrl").asText(null),
					c.path("imageUrl").asText(null), c.path("logoUrl").asText(null));
			String country = c.path("country").asText(null);
			String type = c.path("type").asText(null);

			ConnectorDto connector = new ConnectorDto(connId, name, primaryColor, institutionUrl, country, type);
			return new ItemDetailsDto(id, connector);

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

	private static String safeTrim(String s, int max) {
		if (s == null)
			return "null";
		return s.length() <= max ? s : s.substring(0, max) + "…";
	}

	private static String firstNonEmpty(String... vals) {
		if (vals == null)
			return null;
		for (String v : vals)
			if (v != null && !v.isBlank())
				return v;
		return null;
	}
}