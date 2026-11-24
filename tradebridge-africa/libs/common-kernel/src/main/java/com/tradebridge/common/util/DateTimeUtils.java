package com.tradebridge.common.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * UTC-strict helpers. All persistence and event timestamps are UTC
 * {@code TIMESTAMPTZ} per spec §1.3.
 */
public final class DateTimeUtils {

    public static final ZoneOffset UTC = ZoneOffset.UTC;
    public static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;
    public static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private DateTimeUtils() { }

    public static Instant now() {
        return Instant.now(Clock.systemUTC());
    }

    public static Instant nowTruncatedToMillis() {
        return now().truncatedTo(ChronoUnit.MILLIS);
    }

    public static Instant toUtc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(UTC).toInstant();
    }

    public static OffsetDateTime asUtcOffset(Instant instant) {
        return instant == null ? null : instant.atOffset(UTC);
    }

    public static String formatIso(Instant instant) {
        return instant == null ? null : ISO_INSTANT.format(instant);
    }

    public static Instant parseIso(String text) {
        return text == null || text.isBlank() ? null : Instant.parse(text);
    }

    public static LocalDate todayUtc() {
        return LocalDate.now(Clock.systemUTC());
    }

    public static boolean isFuture(LocalDate date) {
        return date != null && date.isAfter(todayUtc());
    }

    public static boolean isPast(Instant instant) {
        return instant != null && instant.isBefore(now());
    }

    public static boolean isExpired(Instant expiry) {
        return expiry != null && !expiry.isAfter(now());
    }
}