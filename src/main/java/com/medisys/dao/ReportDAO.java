package com.medisys.dao;

import com.medisys.model.ReportPeriod;
import com.medisys.model.ReportRow;
import com.medisys.model.ReportSummary;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Read-only queries that add up the other modules' tables for the reports.
 * Nothing is written here (saved reports are in SavedReportDAO).
 *
 * "Sales" always means orders that were NOT cancelled, counted on the day the
 * order was placed.
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
public interface ReportDAO {

    /** All headline numbers of the period. */
    ReportSummary summary(ReportPeriod period) throws SQLException;

    /** One row per day that had sales: label = yyyy-MM-dd, count = orders, amount = revenue. */
    List<ReportRow> dailySales(ReportPeriod period) throws SQLException;

    /** Best selling medicines by revenue: count = packs, amount = revenue, detail = category. */
    List<ReportRow> topMedicines(ReportPeriod period, int limit) throws SQLException;

    /** Revenue per category: count = packs, amount = revenue. */
    List<ReportRow> salesByCategory(ReportPeriod period) throws SQLException;

    /** Customers who spent the most: count = orders, amount = spent. */
    List<ReportRow> topCustomers(ReportPeriod period, int limit) throws SQLException;

    /** Prescriptions uploaded in the period per status: label = status name, count. */
    List<ReportRow> prescriptionStatuses(ReportPeriod period) throws SQLException;

    /** Per rider: count = delivered, extra = on time, detail = failed attempts (as text). */
    List<ReportRow> riderPerformance(ReportPeriod period) throws SQLException;

    /** Medicines on sale at or below their reorder level: count = stock, extra = reorder level. */
    List<ReportRow> lowStock() throws SQLException;

    /** Medicines on sale that expire within "days" days: count = stock, detail = expiry date. */
    List<ReportRow> expiringSoon(int days) throws SQLException;

    /** Value of all stock on sale (price x quantity). */
    BigDecimal stockValue() throws SQLException;

    int outOfStockCount() throws SQLException;
}
