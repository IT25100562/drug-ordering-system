package com.medisys.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything on the Reports page for one period.
 * Filled by ReportService, shown by report/dashboard.jsp.
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
public class Report {

    private ReportPeriod period;
    private ReportSummary summary = new ReportSummary();

    // Sales
    private List<ReportRow> dailySales = new ArrayList<>();         // label = date, count = orders, amount = revenue
    private List<ReportRow> topMedicines = new ArrayList<>();       // count = packs, amount = revenue, detail = category
    private List<ReportRow> categories = new ArrayList<>();         // count = packs, amount = revenue
    private List<ReportRow> topCustomers = new ArrayList<>();       // count = orders, amount = spent

    // Prescriptions / deliveries
    private List<ReportRow> prescriptionStatuses = new ArrayList<>(); // label = status label, count
    private List<ReportRow> riders = new ArrayList<>();               // count = delivered, extra = on time, detail = failed attempts

    // Inventory (right now, not for the period)
    private List<ReportRow> lowStock = new ArrayList<>();           // count = stock, extra = reorder level, detail = category
    private List<ReportRow> expiringSoon = new ArrayList<>();       // count = stock, detail = expiry date
    private BigDecimal stockValue = BigDecimal.ZERO;
    private int outOfStockCount;

    /** The largest amount in a list (for scaling bars), at least 1 so nothing divides by 0. */
    public static double maxAmount(List<ReportRow> rows) {
        double max = 0;
        for (ReportRow r : rows) {
            if (r.getAmount() != null) {
                max = Math.max(max, r.getAmount().doubleValue());
            }
        }
        return max > 0 ? max : 1;
    }

    /** The largest count in a list, at least 1. */
    public static int maxCount(List<ReportRow> rows) {
        int max = 0;
        for (ReportRow r : rows) {
            max = Math.max(max, r.getCount());
        }
        return max > 0 ? max : 1;
    }

    public ReportPeriod getPeriod() {
        return period;
    }

    public void setPeriod(ReportPeriod period) {
        this.period = period;
    }

    public ReportSummary getSummary() {
        return summary;
    }

    public void setSummary(ReportSummary summary) {
        this.summary = summary;
    }

    public List<ReportRow> getDailySales() {
        return dailySales;
    }

    public void setDailySales(List<ReportRow> dailySales) {
        this.dailySales = dailySales;
    }

    public List<ReportRow> getTopMedicines() {
        return topMedicines;
    }

    public void setTopMedicines(List<ReportRow> topMedicines) {
        this.topMedicines = topMedicines;
    }

    public List<ReportRow> getCategories() {
        return categories;
    }

    public void setCategories(List<ReportRow> categories) {
        this.categories = categories;
    }

    public List<ReportRow> getTopCustomers() {
        return topCustomers;
    }

    public void setTopCustomers(List<ReportRow> topCustomers) {
        this.topCustomers = topCustomers;
    }

    public List<ReportRow> getPrescriptionStatuses() {
        return prescriptionStatuses;
    }

    public void setPrescriptionStatuses(List<ReportRow> prescriptionStatuses) {
        this.prescriptionStatuses = prescriptionStatuses;
    }

    public List<ReportRow> getRiders() {
        return riders;
    }

    public void setRiders(List<ReportRow> riders) {
        this.riders = riders;
    }

    public List<ReportRow> getLowStock() {
        return lowStock;
    }

    public void setLowStock(List<ReportRow> lowStock) {
        this.lowStock = lowStock;
    }

    public List<ReportRow> getExpiringSoon() {
        return expiringSoon;
    }

    public void setExpiringSoon(List<ReportRow> expiringSoon) {
        this.expiringSoon = expiringSoon;
    }

    public BigDecimal getStockValue() {
        return stockValue;
    }

    public void setStockValue(BigDecimal stockValue) {
        this.stockValue = stockValue;
    }

    public int getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(int outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }
}
