package com.juneba.erp;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.juneba.erp.config.PluggyProperties;
import com.juneba.erp.service.PluggyAuthService;


@ExtendWith(MockitoExtension.class)
public class PluggyAuthServiceTest {

    @Mock private OkHttpClient httpClient;
    @Mock private Call call;
    @Mock private Response response;
    @Mock private ResponseBody body;
    @InjectMocks private PluggyAuthService service;

    private PluggyProperties props;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        props = new PluggyProperties();
        props.setBaseUrl("https://api.pluggy.ai");
        props.setClientId("abc");
        props.setClientSecret("123");
        service = new PluggyAuthService(httpClient, props);
    }

    @Test
    void deveRetornarApiKeyComSucesso() throws Exception {
        String json = "{\"apiKey\":\"XYZ123\"}";

        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(body);
        when(body.string()).thenReturn(json);

        String key = service.getApiKey();

        assertEquals("XYZ123", key);
        assertTrue(key.length() > 0);
    }

    @Test
    void deveLancarExcecaoQuandoFalhaHttp() throws Exception {
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(false);
        when(response.code()).thenReturn(500);
        when(response.body()).thenReturn(body);
        when(body.string()).thenReturn("erro");

        assertThrows(IllegalStateException.class, () -> service.getApiKey());
    }
}