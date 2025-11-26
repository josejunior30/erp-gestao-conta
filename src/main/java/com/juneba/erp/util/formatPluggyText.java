package com.juneba.erp.util;

public class formatPluggyText {

	public static String last4(String... candidates) {
        for (String c : candidates) {
            if (c != null) {
                String digits = c.replaceAll("\\D+", "");
                if (digits.length() >= 4) return digits.substring(digits.length() - 4);
            }
        }
        return null;
    }

    public static String firstNonEmpty(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    public static String safeTrim(String s, int max) {
        if (s == null) return "null";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}