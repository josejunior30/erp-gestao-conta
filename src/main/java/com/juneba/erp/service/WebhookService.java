package com.juneba.erp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import com.juneba.erp.entities.PluggyItem;
import com.juneba.erp.repository.PluggyItemRepository;
import java.util.Map;

@Service
public class WebhookService {
  private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

  public enum Outcome { ACCEPTED_NO_ITEM, INSERTED, ALREADY_EXISTS }

  private final PluggyItemRepository repository;

  public WebhookService(PluggyItemRepository repository) {
    this.repository = repository;
  }

  public Outcome handle(Map<String, Object> payload, Map<String, String> headers) {
    final String itemId = extractItemId(payload);

    if (itemId == null || itemId.isBlank()) {
      // por quê: 202 evita "sucesso" enganoso, mantendo observabilidade
      log.info("Webhook sem itemId; headersKeys={}, payloadKeys={}", headers.keySet(), payload.keySet());
      return Outcome.ACCEPTED_NO_ITEM;
    }

    if (!repository.existsByItemId(itemId)) {
      try {
    	  repository.save(new PluggyItem(itemId));
        log.info("Pluggy item persistido: itemId={}", itemId);
        return Outcome.INSERTED;
      } catch (DataIntegrityViolationException dup) {
      
        log.info("Item já existia (race): itemId={}", itemId);
        return Outcome.ALREADY_EXISTS;
      }
    } else {
      log.debug("Item já cadastrado: itemId={}", itemId);
      return Outcome.ALREADY_EXISTS;
    }
  }

  @SuppressWarnings("unchecked")
  static String extractItemId(Map<String, Object> root) {
    Object v;
    v = root.get("itemId"); if (v != null) return v.toString();
    v = root.get("item_id"); if (v != null) return v.toString();

    Object item = root.get("item");
    if (item instanceof Map) {
      Object id = ((Map<String, Object>) item).get("id");
      if (id != null) return id.toString();
    } else if (item != null) {
      return item.toString();
    }

    Object data = root.get("data");
    if (data instanceof Map) {
      Map<String,Object> d = (Map<String,Object>) data;
      v = d.get("itemId"); if (v != null) return v.toString();
      v = d.get("item_id"); if (v != null) return v.toString();
      Object di = d.get("item");
      if (di instanceof Map) {
        Object id = ((Map<String,Object>) di).get("id");
        if (id != null) return id.toString();
      } else if (di != null) {
        return di.toString();
      }
    }

    v = root.get("resourceId"); if (v != null) return v.toString();
    return null;
  }
}