package com.juneba.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ai.pluggy.client.PluggyClient;
import okhttp3.OkHttpClient;

@Configuration
public class HttpClientsConfig {

  @Bean
  public OkHttpClient okHttpClient() {
    return new OkHttpClient.Builder().build();
  }

  @Bean
  public PluggyClient pluggyClient(PluggyProperties props) {
    return PluggyClient.builder()
      .clientIdAndSecret(props.getClientId(), props.getClientSecret())
      .build();
  }
}