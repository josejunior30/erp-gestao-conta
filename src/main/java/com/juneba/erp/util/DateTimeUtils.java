package com.juneba.erp.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.JsonNode;

public final class DateTimeUtils {
    private DateTimeUtils() {}

    public static Instant parseInstantFlexible(String s, ZoneId zone) {
        if (s == null || s.isBlank()) return null;
        try { return OffsetDateTime.parse(s).toInstant(); } catch (Exception ignored) {}
        try { return Instant.parse(s); } catch (Exception ignored) {}
        try { return LocalDateTime.parse(s).atZone(zone).toInstant(); } catch (Exception ignored) {}
        try { return LocalDate.parse(s).atStartOfDay(zone).toInstant(); } catch (Exception ignored) {}
        return null;
    }
    
    public static Instant extractInstant(JsonNode t, ZoneId zone) {
        String raw = formatPluggyText.firstNonEmpty(
                t.path("time").asText(null),
                t.path("date").asText(null),
                t.path("postedAt").asText(null)
        );
        return DateTimeUtils.parseInstantFlexible(raw, zone);
    }
}