package com.medisys.report;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The dates a report covers: from "from" to "to", both days included.
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
public class ReportPeriod {

    public static final String DEFAULT_PRESET = "30";

    /** Preset key -> label, in the order the buttons are shown. */
    public static final Map<String, String> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("7", "Last 7 days");
        PRESETS.put("30", "Last 30 days");
        PRESETS.put("90", "Last 90 days");
        PRESETS.put("month", "This month");
        PRESETS.put("lastmonth", "Last month");
        PRESETS.put("year", "This year");
    }

    /** The period of a preset, ending today. */
    public static ReportPeriod preset(String key) {
        LocalDate today = LocalDate.now();
        switch (key) {
            case "7":
                return new ReportPeriod(today.minusDays(6), today, key);
            case "90":
                return new ReportPeriod(today.minusDays(89), today, key);
            case "month":
                return new ReportPeriod(today.withDayOfMonth(1), today, key);
            case "lastmonth": {
                LocalDate first = today.withDayOfMonth(1).minusMonths(1);
                return new ReportPeriod(first, first.plusMonths(1).minusDays(1), key);
            }
            case "year":
                return new ReportPeriod(today.withDayOfYear(1), today, key);
            default:
                return new ReportPeriod(today.minusDays(29), today, "30");
        }
    }

    private static final DateTimeFormatter NICE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final LocalDate from;
    private final LocalDate to;
    private final String preset;      // "30", "month", ... or "custom"

    public ReportPeriod(LocalDate from, LocalDate to, String preset) {
        this.from = from;
        this.to = to;
        this.preset = preset;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    /** The day after "to" - SQL uses  created_at >= from AND created_at < toExclusive. */
    public LocalDate getToExclusive() {
        return to.plusDays(1);
    }

    public String getPreset() {
        return preset;
    }

    /** Number of days in the period (both ends included). */
    public int getDays() {
        return (int) ChronoUnit.DAYS.between(from, to) + 1;
    }

    /** "1 Sep 2026 - 17 Sep 2026" */
    public String getLabel() {
        return from.equals(to) ? from.format(NICE) : from.format(NICE) + " - " + to.format(NICE);
    }

    /** "from=2026-09-01&to=2026-09-17", for links that keep the same period. */
    public String getQuery() {
        return "from=" + from + "&to=" + to;
    }
}
