package com.medisys.model;

import java.math.BigDecimal;

/**
 * One line of a report table or chart, e.g. a medicine with how many packs
 * were sold and for how much, or one day with its revenue.
 *
 * The same small class is used by every report, so the pages and the CSV
 * export can treat all reports alike:
 *   label    - what the line is about ("Panadol 500 mg", "2026-09-17", "Ruwan")
 *   detail   - extra text ("Pain Relief", "expires 2026-10-01"), may be null
 *   count    - a whole number (packs, orders, deliveries ...)
 *   amount   - money, may be null
 *   extra    - a second number (e.g. on-time deliveries), may be 0
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
public class ReportRow {

    private String label;
    private String detail;
    private int count;
    private BigDecimal amount;
    private int extra;

    public ReportRow() {
    }

    public ReportRow(String label, String detail, int count, BigDecimal amount, int extra) {
        this.label = label;
        this.detail = detail;
        this.count = count;
        this.amount = amount;
        this.extra = extra;
    }

    /** extra as a percentage of count (e.g. on-time share), or null when count is 0. */
    public Double getExtraPercent() {
        return count == 0 ? null : extra * 100.0 / count;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getExtra() {
        return extra;
    }

    public void setExtra(int extra) {
        this.extra = extra;
    }
}
