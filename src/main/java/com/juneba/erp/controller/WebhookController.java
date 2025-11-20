package com.juneba.erp.controller;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.juneba.erp.service.WebhookService;



@RestController
@RequestMapping("/pluggy/webhook")
public class WebhookController {


  private final WebhookService service;

  public WebhookController(WebhookService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> receive(@RequestBody Map<String, Object> payload,
                                      @RequestHeader Map<String, String> headers) {

    WebhookService.Outcome out = service.handle(payload, headers);

    return switch (out) {
      case ACCEPTED_NO_ITEM -> ResponseEntity.accepted().build();
      case INSERTED, ALREADY_EXISTS -> ResponseEntity.ok().build();
    };
  }
}