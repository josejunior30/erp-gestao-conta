package com.juneba.erp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.juneba.erp.config.PluggyProperties;
import com.juneba.erp.service.PluggyAuthService;
import com.juneba.erp.service.PluggyConnectTokenService;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PluggyConnectTokenServiceTest {

    @Mock private OkHttpClient http;
    @Mock private PluggyAuthService auth;
    @Mock private Call call;
    @Mock private Response response;
    @Mock private ResponseBody body;

    private PluggyProperties props;
    private PluggyConnectTokenService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        props = new PluggyProperties();
        props.setBaseUrl("https://api.pluggy.ai");
        props.setClientId("client");
        props.setClientSecret("secret");
        service = new PluggyConnectTokenService(http, auth, props);
    }

    @Test
    void deveCriarConnectTokenComSucesso() throws Exception {
        when(auth.getApiKey()).thenReturn("apikey");
        when(http.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(body);
        when(body.string()).thenReturn("{\"connectToken\":\"TOKEN123\"}");

        String token = service.createConnectToken("user123", true, null, null);
        assertEquals("TOKEN123", token);
    }

    @Test
    void deveLancarExcecaoQuandoFalhaHttp() throws Exception {
        when(auth.getApiKey()).thenReturn("apikey");
        when(http.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);
        when(response.isSuccessful()).thenReturn(false);
        when(response.code()).thenReturn(400);
        when(response.body()).thenReturn(body);
        when(body.string()).thenReturn("erro");

        assertThrows(IllegalStateException.class,
                () -> service.createConnectToken("user", false, null, null));
    }
} 