package com.ghostqueue.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {
    private static final DateTimeFormatter DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");
    private static final DateTimeFormatter TIME_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private DateTimeUtils() {}

    public static String formatHeaderDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_DISPLAY_FORMAT);
    }

    public static String formatTime(LocalTime time) {
        if (time == null) return "";
        return time.format(TIME_DISPLAY_FORMAT);
    }

    public static String formatShortDate(LocalDate date) {
        if (date == null) return "";
        return date.format(SHORT_DATE_FORMAT);
    }

    public static String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"));
    }
}
