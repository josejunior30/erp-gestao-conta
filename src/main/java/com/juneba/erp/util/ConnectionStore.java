package com.juneba.erp.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ConnectionStore {
  private final Map<String,String> map = new ConcurrentHashMap<>();
  public void put(String clientUserId, String itemId) { if (clientUserId!=null && itemId!=null) map.put(clientUserId,itemId); }
  public String getItemId(String clientUserId) { return map.get(clientUserId); }
}
