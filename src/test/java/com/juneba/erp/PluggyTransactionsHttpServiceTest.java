package com.juneba.erp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.juneba.erp.DTO.TransactionSummaryDto;
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
	void deveBuscarTransacoesPorAccountId_Cru() throws Exception {
		when(auth.getApiKey()).thenReturn("api-key");

		Call call = mock(Call.class);
		Response response = mock(Response.class);
		ResponseBody body = mock(ResponseBody.class);

		when(client.newCall(any())).thenReturn(call);
		when(call.execute()).thenReturn(response);
		when(response.isSuccessful()).thenReturn(true);
		when(response.body()).thenReturn(body);
		when(body.string()).thenReturn("{\"results\":[{\"id\":\"tx1\"},{\"id\":\"tx2\"}]}");

		ObjectNode result = service.fetchAllTransactionsByAccountId("acc-123", LocalDate.now().minusDays(5),
				LocalDate.now(), null, 10);

		assertNotNull(result);
		assertEquals("acc-123", result.get("accountId").asText());
		assertEquals(2, result.get("count").asInt());
		assertTrue(result.get("transactions").isArray());
		assertEquals(2, result.get("transactions").size());
	}

	@Test
	void deveBuscarTransacoesPorAccountId_Pretty() throws Exception {
		when(auth.getApiKey()).thenReturn("api-key");

		Call call1 = mock(Call.class);
		Call call2 = mock(Call.class);

		Response resp1 = mock(Response.class);
		ResponseBody body1 = mock(ResponseBody.class);

		Response resp2 = mock(Response.class);
		ResponseBody body2 = mock(ResponseBody.class);

		when(client.newCall(any())).thenReturn(call1, call2);

		when(call1.execute()).thenReturn(resp1);
		when(resp1.isSuccessful()).thenReturn(true);
		when(resp1.body()).thenReturn(body1);
		when(body1.string()).thenReturn("{\"id\":\"acc-123\",\"name\":\"Conta X\",\"type\":\"BANK\"}");

		when(call2.execute()).thenReturn(resp2);
		when(resp2.isSuccessful()).thenReturn(true);
		when(resp2.body()).thenReturn(body2);
		when(body2.string())
				.thenReturn("{\"results\":[{\"id\":\"tx1\",\"amount\":100},{\"id\":\"tx2\",\"amount\":-50}]}");

		TransactionSummaryDto dto = service.fetchAllTransactionsByAccountIdPretty("acc-123",
				LocalDate.now().minusDays(30), LocalDate.now(), null, 10);

		assertNotNull(dto);
		assertEquals("acc-123", dto.itemId()); 
		assertEquals(2, dto.totalCount());
		assertEquals(100, dto.totalInflow().intValue());
		assertEquals(50, dto.totalOutflow().intValue());
		assertEquals(50, dto.net().intValue());
		assertNotNull(dto.transactions());
		assertEquals(2, dto.transactions().size());
	}
}
