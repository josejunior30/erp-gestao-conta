package com.juneba.erp.util;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;

public final class JsonNodeUtils {
    private JsonNodeUtils() { /* evitar instância */ }

    /** Lê BigDecimal de JsonNode com tolerância a string/vazio/inválido. */
    public static BigDecimal asBigDecimal(JsonNode obj, String field) {
        if (obj == null || field == null) return null;
        JsonNode n = obj.path(field);
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        if (n.isNumber()) return n.decimalValue();
        String s = n.asText(null);
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ex) {
            return null; // dado inválido no upstream
        }
    }
}