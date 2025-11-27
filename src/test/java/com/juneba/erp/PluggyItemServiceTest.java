package com.juneba.erp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.juneba.erp.Exception.UpstreamException;
import com.juneba.erp.config.PluggyProperties;
import com.juneba.erp.repository.PluggyItemRepository;
import com.juneba.erp.service.PluggyAuthService;
import com.juneba.erp.service.PluggyItemService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import static org.junit.jupiter.api.Assertions.*;

public class PluggyItemServiceTest {

	@Mock
	private OkHttpClient client;
	@Mock
	private PluggyAuthService auth;
	@Mock
	private PluggyItemRepository repo;

	@Mock
	private Call call;
	@Mock
	private Response response;
	@Mock
	private ResponseBody body;

	private PluggyProperties props;
	private PluggyItemService service;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		props = new PluggyProperties();
		props.setBaseUrl("https://api.pluggy.ai");

		service = new PluggyItemService(client, auth, props, repo);
	}

	@Test
	void deveBuscarItemComSucesso() throws Exception {
		when(auth.getApiKey()).thenReturn("apikey");
		when(client.newCall(any())).thenReturn(call);
		when(call.execute()).thenReturn(response);
		when(response.isSuccessful()).thenReturn(true);
		when(response.body()).thenReturn(body);

		when(body.string()).thenReturn("""
				{
				  "id": "item123",
				  "connector": {
				    "id": "c1",
				    "name": "Banco XPTO",
				    "primaryColor": "#123456",
				    "institutionUrl": "https://bank.com"
				  }
				}
				""").thenReturn(
				"""
						{ "results": [ { "id": "acc1", "name":"Conta 1", "type":"BANK", "currencyCode":"BRL", "balance": 100.00, "availableBalance": 90.00 } ] }
						""");

		var dto = service.fetchItemDetails("item123");

		assertEquals("item123", dto.id());
		assertEquals("Banco XPTO", dto.connector().name());
		assertNotNull(dto.accounts());
		assertEquals(1, dto.accounts().size());
		assertEquals("acc1", dto.accounts().get(0).id());
	}

	@Test
	void deveLancarExcecaoQuandoFalhaHttp() throws Exception {
		when(auth.getApiKey()).thenReturn("apikey");
		when(client.newCall(any())).thenReturn(call);
		when(call.execute()).thenReturn(response);
		when(response.isSuccessful()).thenReturn(false);
		when(response.code()).thenReturn(500);
		when(response.body()).thenReturn(body);
		when(body.string()).thenReturn("erro");

		assertThrows(UpstreamException.class, () -> service.fetchItemDetails("item123"));
	}
}
