package com.juneba.erp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.juneba.erp.config.PluggyProperties;
import com.juneba.erp.service.PluggyAuthService;
import com.juneba.erp.service.PluggyTransactionsHttpService;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PluggyTransactionsHttpServiceTest {

	@Mock
	private OkHttpClient client;
	@Mock
	private PluggyAuthService auth;
	@Mock
	private Call call;
	@Mock
	private Response response;
	@Mock
	private ResponseBody body;

	private PluggyProperties props;
	private PluggyTransactionsHttpService service;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		props = new PluggyProperties();
		props.setBaseUrl("https://api.pluggy.ai");
		props.setClientId("x");
		props.setClientSecret("y");
		service = new PluggyTransactionsHttpService(client, auth, props);
	}

	@Test
	void deveBuscarTransacoesPorItemId() throws Exception {
		when(auth.getApiKey()).thenReturn("api-key");

		// Simula chamadas /accounts e /transactions
		when(client.newCall(any())).thenReturn(call);
		when(call.execute()).thenReturn(response);
		when(response.isSuccessful()).thenReturn(true);
		when(response.body()).thenReturn(body);
		when(body.string()).thenReturn("{\"results\": [{\"id\":\"acc1\"}]}")
				.thenReturn("{\"results\": [{\"id\":\"tx1\"},{\"id\":\"tx2\"}]}");

		var result = service.fetchAllTransactionsByItemId("item123", LocalDate.now().minusDays(5), LocalDate.now(),
				null, 10);
		assertEquals("item123", result.get("itemId").asText());
		assertTrue(result.get("count").asInt() >= 1);
	}
}
