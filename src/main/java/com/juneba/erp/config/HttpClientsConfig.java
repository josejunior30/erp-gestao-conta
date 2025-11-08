package com.juneba.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ai.pluggy.client.PluggyClient;
import ai.pluggy.client.PluggyClient.PluggyClientBuilder;
import okhttp3.OkHttpClient;

@Configuration
public class HttpClientsConfig {

  @Bean
  public OkHttpClient okHttpClient() {
    return new OkHttpClient.Builder().build();
  }

  @Bean
  public PluggyClient pluggyClient(PluggyProperties props) {
    PluggyClientBuilder b = PluggyClient.builder()
        .clientIdAndSecret(props.getClientId(), props.getClientSecret());
    if (props.getBaseUrl() != null && !props.getBaseUrl().isBlank()) {
      b.baseUrl(props.getBaseUrl());
    }
    return b.build();
  }
}