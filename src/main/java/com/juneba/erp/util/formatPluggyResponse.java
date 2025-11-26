package com.juneba.erp.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;

public class formatPluggyResponse {
	
	
	 private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
	
	public static String formatCurrency(BigDecimal amount, String currencyCode) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(PT_BR);
        try {
            if (currencyCode != null) {
                nf.setCurrency(Currency.getInstance(currencyCode));
            }
        } catch (Exception ignored) { } // Por quê: fallback em currency inválida
        if (amount == null) amount = BigDecimal.ZERO;
        return nf.format(amount);
    }

    public static BigDecimal safeBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) return BigDecimal.ZERO;
        try {
            if (node.isNumber()) return node.decimalValue();
            String s = node.asText(null);
            if (s == null || s.isBlank()) return BigDecimal.ZERO;
            return new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

}
