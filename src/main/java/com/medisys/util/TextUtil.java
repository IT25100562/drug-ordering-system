package com.medisys.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Small text helpers shared by all modules.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public final class TextUtil {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TextUtil() {
        // utility class, no objects
    }

    /**
     * Escapes text before it is printed inside a JSP page, so a value like
     * "<script>" is shown as text and never runs.
     */
    public static String html(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    /** Same as html(String) for any object (numbers, enums ...). */
    public static String html(Object value) {
        return value == null ? "" : html(String.valueOf(value));
    }

    /** Trims a request parameter and turns null into "". */
    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Parses a whole number, or returns null when the text is not one. */
    public static Integer parseInt(String value) {
        try {
            return Integer.valueOf(clean(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parses a decimal number, or returns null when the text is not one. */
    public static BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(clean(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parses a yyyy-MM-dd date (what an HTML date input sends), or returns null. */
    public static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(clean(value));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * True for a path inside this app such as "/medicines?q=pan", so it is safe
     * to redirect to. Blocks "//evil.com", "http://..." and line breaks.
     */
    public static boolean isSafeLocalPath(String path) {
        return path != null
                && path.startsWith("/")
                && !path.startsWith("//")
                && path.length() <= 300
                && path.matches("[A-Za-z0-9/_\\-.?=&%+]*");
    }

    /** Shows money as "Rs. 1,250.00". */
    public static String money(BigDecimal amount) {
        return amount == null ? "-" : String.format("Rs. %,.2f", amount);
    }

    public static String date(LocalDate date) {
        return date == null ? "-" : date.toString();
    }

    public static String dateTime(LocalDateTime time) {
        return time == null ? "-" : time.format(DATE_TIME_FORMAT);
    }

    /** "just now", "5 min ago", "3 hours ago", "2 days ago". */
    public static String timeAgo(LocalDateTime time) {
        if (time == null) {
            return "-";
        }
        long minutes = java.time.Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + " min ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }
        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }
}
