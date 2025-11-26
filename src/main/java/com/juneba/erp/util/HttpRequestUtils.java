package com.juneba.erp.util;

import jakarta.servlet.http.HttpServletRequest;

public final class HttpRequestUtils {
    private HttpRequestUtils() {}
    public static String clientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        return (h != null && !h.isBlank()) ? h.split(",")[0].trim() : req.getRemoteAddr();
    }
}
