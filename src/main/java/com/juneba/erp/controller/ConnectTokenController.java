package com.juneba.erp.controller;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.juneba.erp.DTO.CreateTokenRequest;
import com.juneba.erp.service.PluggyConnectTokenService;

import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/api/pluggy")
public class ConnectTokenController {
	private static final Logger log = LoggerFactory.getLogger(ConnectTokenController.class);
 

  private final PluggyConnectTokenService service;

  public ConnectTokenController(PluggyConnectTokenService service) {
    this.service = service;
  }
  @PostMapping(value = "/connect-token", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, String> create(@RequestBody(required = false) CreateTokenRequest body,
                                    HttpServletRequest req) {
    final String reqId = UUID.randomUUID().toString().substring(0, 8);
    final long t0 = System.nanoTime();
    final String clientUserId = (body != null) ? body.clientUserId() : null;
    final boolean avoid = body != null && Boolean.TRUE.equals(body.avoidDuplicates());
    final String itemId = (body != null) ? body.itemId() : null;
    final String redirect = (body != null) ? body.oauthRedirectUri() : null;

    final String xff = req.getHeader("X-Forwarded-For");
    final String ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    final boolean hasRedirect = (redirect != null && !redirect.isBlank());

    log.info("connect-token IN [{}] ip={} clientUserId='{}' avoidDuplicates={} itemId='{}' hasRedirect={}",
        reqId, ip, (clientUserId != null ? clientUserId : ""), avoid, (itemId != null ? itemId : ""), hasRedirect);
    try {
      final String token = service.createConnectToken(clientUserId, avoid, itemId, redirect);
      final long ms = (System.nanoTime() - t0) / 1_000_000;

      log.info("connect-token OK [{}] {} ms", reqId, ms);
      return Map.of("connectToken", token);

    } catch (RuntimeException ex) {
      final long ms = (System.nanoTime() - t0) / 1_000_000;
      log.error("connect-token ERR [{}] {} ms: {}", reqId, ms, ex.getMessage(), ex);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao criar connect token");
    }
  }
}