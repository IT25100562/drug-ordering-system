package com.medisys.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * The dates a report covers: from "from" to "to", both days included.
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
public class ReportPeriod {

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
