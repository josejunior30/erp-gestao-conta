package com.juneba.erp.controller;



import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/webhooks/pluggy")
public class WebhookController {
  private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

  @PostMapping
  public ResponseEntity<Void> receive(@RequestBody Map<String, Object> payload,
                                      @RequestHeader Map<String, String> headers) {
    log.info("Webhook Pluggy recebido: headers={}, payload={}", headers, payload);
    return ResponseEntity.ok().build();
  }
}
