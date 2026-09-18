package com.medisys.dao.impl;

import com.medisys.config.DBConnection;
import com.medisys.dao.ReportDAO;
import com.medisys.model.PrescriptionStatus;
import com.medisys.model.ReportPeriod;
import com.medisys.model.ReportRow;
import com.medisys.model.ReportSummary;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of ReportDAO (SQL Server).
 *
 * Every query uses GROUP BY / COUNT / SUM so the database does the adding up,
 * and a date range  "x >= from AND x < the day after to"  so the last day
 * is fully included whatever the time of day.
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
public class ReportDAOImpl implements ReportDAO {

    /** Orders placed in the period that were not cancelled (alias o). */
    private static final String SOLD = "o.status <> 'CANCELLED' AND o.created_at >= ? AND o.created_at < ? ";

    // ============================================================ summary

    @Override
    public ReportSummary summary(ReportPeriod p) throws SQLException {
        ReportSummary s = new ReportSummary();
        try (Connection con = DBConnection.getInstance().getConnection()) {

            // 1. Sales and cancellations in one pass over the orders of the period.
            String sales = "SELECT "
                    + "SUM(CASE WHEN status <> 'CANCELLED' THEN 1 ELSE 0 END) AS orders, "
                    + "SUM(CASE WHEN status <> 'CANCELLED' THEN total ELSE 0 END) AS revenue, "
                    + "SUM(CASE WHEN status <> 'CANCELLED' AND source = 'CART' THEN total ELSE 0 END) AS cart_revenue, "
                    + "SUM(CASE WHEN status <> 'CANCELLED' AND source = 'PRESCRIPTION' THEN total ELSE 0 END) AS rx_revenue, "
                    + "SUM(CASE WHEN status <> 'CANCELLED' THEN delivery_fee ELSE 0 END) AS fees, "
                    + "SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled, "
                    + "SUM(CASE WHEN status = 'CANCELLED' THEN total ELSE 0 END) AS refunded, "
                    + "COUNT(DISTINCT CASE WHEN status <> 'CANCELLED' THEN user_id END) AS buyers "
                    + "FROM orders WHERE created_at >= ? AND created_at < ?";
            try (PreparedStatement ps = con.prepareStatement(sales)) {
                setPeriod(ps, 1, p);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    s.setOrderCount(rs.getInt("orders"));
                    s.setRevenue(money(rs.getBigDecimal("revenue")));
                    s.setCartRevenue(money(rs.getBigDecimal("cart_revenue")));
                    s.setPrescriptionRevenue(money(rs.getBigDecimal("rx_revenue")));
                    s.setDeliveryFees(money(rs.getBigDecimal("fees")));
                    s.setCancelledCount(rs.getInt("cancelled"));
                    s.setRefundedAmount(money(rs.getBigDecimal("refunded")));
                    s.setBuyingCustomers(rs.getInt("buyers"));
                }
            }

            // 2. New customer accounts.
            s.setNewCustomers(count(con, "SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER' "
                    + "AND created_at >= ? AND created_at < ?", p));

            // 3. Prescriptions uploaded in the period, per outcome.
            String rx = "SELECT COUNT(*) AS total, "
                    + "SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS approved, "
                    + "SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected, "
                    + "SUM(CASE WHEN status = 'CORRECTION_REQUESTED' THEN 1 ELSE 0 END) AS correction, "
                    + "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending, "
                    + "SUM(CASE WHEN order_id IS NOT NULL THEN 1 ELSE 0 END) AS paid, "
                    // average time from upload to the pharmacist's decision, in hours
                    + "AVG(CASE WHEN reviewed_at IS NOT NULL "
                    + "    THEN CAST(DATEDIFF(minute, uploaded_at, reviewed_at) AS FLOAT) / 60 END) AS review_hours "
                    + "FROM prescriptions WHERE uploaded_at >= ? AND uploaded_at < ?";
            try (PreparedStatement ps = con.prepareStatement(rx)) {
                setPeriod(ps, 1, p);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    s.setPrescriptionCount(rs.getInt("total"));
                    s.setApprovedCount(rs.getInt("approved"));
                    s.setRejectedCount(rs.getInt("rejected"));
                    s.setCorrectionCount(rs.getInt("correction"));
                    s.setPendingCount(rs.getInt("pending"));
                    s.setPaidPrescriptionCount(rs.getInt("paid"));
                    double hours = rs.getDouble("review_hours");
                    s.setAverageReviewHours(rs.wasNull() ? null : hours);
                }
            }

            // 4. Deliveries finished in the period, and how many were on time.
            String del = "SELECT COUNT(*) AS delivered, "
                    + "SUM(CASE WHEN CAST(delivered_at AS DATE) <= estimated_date THEN 1 ELSE 0 END) AS on_time "
                    + "FROM deliveries WHERE status = 'DELIVERED' AND delivered_at >= ? AND delivered_at < ?";
            try (PreparedStatement ps = con.prepareStatement(del)) {
                setPeriod(ps, 1, p);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    s.setDeliveredCount(rs.getInt("delivered"));
                    s.setOnTimeCount(rs.getInt("on_time"));
                }
            }
            s.setFailedAttempts(count(con, "SELECT COUNT(*) FROM delivery_updates WHERE status = 'FAILED' "
                    + "AND created_at >= ? AND created_at < ?", p));
        }
        return s;
    }

    // ============================================================== sales

    @Override
    public List<ReportRow> dailySales(ReportPeriod p) throws SQLException {
        String sql = "SELECT CAST(o.created_at AS DATE) AS day, COUNT(*) AS orders, SUM(o.total) AS revenue "
                   + "FROM orders o WHERE " + SOLD
                   + "GROUP BY CAST(o.created_at AS DATE) ORDER BY day";
        List<ReportRow> rows = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setPeriod(ps, 1, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ReportRow(rs.getDate("day").toLocalDate().toString(), null,
                            rs.getInt("orders"), rs.getBigDecimal("revenue"), 0));
                }
            }
        }
        return rows;
    }

    @Override
    public List<ReportRow> topMedicines(ReportPeriod p, int limit) throws SQLException {
        // The name is taken from the order line (as it was sold), the category from the catalog.
        String sql = "SELECT TOP (?) i.medicine_name, c.name AS category, SUM(i.quantity) AS packs, "
                   + "SUM(i.quantity * i.unit_price) AS revenue "
                   + "FROM order_items i "
                   + "JOIN orders o ON o.id = i.order_id "
                   + "JOIN medicines m ON m.id = i.medicine_id "
                   + "JOIN categories c ON c.id = m.category_id "
                   + "WHERE " + SOLD
                   + "GROUP BY i.medicine_id, i.medicine_name, c.name "
                   + "ORDER BY revenue DESC, packs DESC";
        return rows(sql, limit, p, "medicine_name", "category", "packs", "revenue", null);
    }

    @Override
    public List<ReportRow> salesByCategory(ReportPeriod p) throws SQLException {
        String sql = "SELECT c.name, SUM(i.quantity) AS packs, SUM(i.quantity * i.unit_price) AS revenue "
                   + "FROM order_items i "
                   + "JOIN orders o ON o.id = i.order_id "
                   + "JOIN medicines m ON m.id = i.medicine_id "
                   + "JOIN categories c ON c.id = m.category_id "
                   + "WHERE " + SOLD
                   + "GROUP BY c.name ORDER BY revenue DESC";
        return rows(sql, null, p, "name", null, "packs", "revenue", null);
    }

    @Override
    public List<ReportRow> topCustomers(ReportPeriod p, int limit) throws SQLException {
        String sql = "SELECT TOP (?) u.full_name, COUNT(*) AS orders, SUM(o.total) AS spent "
                   + "FROM orders o JOIN users u ON u.id = o.user_id "
                   + "WHERE " + SOLD
                   + "GROUP BY u.id, u.full_name ORDER BY spent DESC";
        return rows(sql, limit, p, "full_name", null, "orders", "spent", null);
    }

    // ============================================ prescriptions / delivery

    @Override
    public List<ReportRow> prescriptionStatuses(ReportPeriod p) throws SQLException {
        String sql = "SELECT status, COUNT(*) AS n FROM prescriptions "
                   + "WHERE uploaded_at >= ? AND uploaded_at < ? GROUP BY status";
        // Always show every status in the same order, also the ones with 0.
        List<ReportRow> rows = new ArrayList<>();
        for (PrescriptionStatus status : PrescriptionStatus.values()) {
            rows.add(new ReportRow(status.name(), status.getLabel(), 0, null, 0));
        }
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setPeriod(ps, 1, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    for (ReportRow row : rows) {
                        if (row.getLabel().equals(rs.getString("status"))) {
                            row.setCount(rs.getInt("n"));
                        }
                    }
                }
            }
        }
        return rows;
    }

    @Override
    public List<ReportRow> riderPerformance(ReportPeriod p) throws SQLException {
        // Every delivery staff member, with sub-queries for their numbers in the period.
        String sql = "SELECT u.full_name, "
                   + "(SELECT COUNT(*) FROM deliveries d WHERE d.staff_id = u.id AND d.status = 'DELIVERED' "
                   + "   AND d.delivered_at >= ? AND d.delivered_at < ?) AS delivered, "
                   + "(SELECT COUNT(*) FROM deliveries d WHERE d.staff_id = u.id AND d.status = 'DELIVERED' "
                   + "   AND d.delivered_at >= ? AND d.delivered_at < ? "
                   + "   AND CAST(d.delivered_at AS DATE) <= d.estimated_date) AS on_time, "
                   + "(SELECT COUNT(*) FROM delivery_updates du WHERE du.updated_by = u.id AND du.status = 'FAILED' "
                   + "   AND du.created_at >= ? AND du.created_at < ?) AS failed "
                   + "FROM users u WHERE u.role = 'DELIVERY_STAFF' "
                   + "ORDER BY delivered DESC, u.full_name";
        List<ReportRow> rows = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setPeriod(ps, 1, p);
            setPeriod(ps, 3, p);
            setPeriod(ps, 5, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ReportRow(rs.getString("full_name"), String.valueOf(rs.getInt("failed")),
                            rs.getInt("delivered"), null, rs.getInt("on_time")));
                }
            }
        }
        return rows;
    }

    // ========================================================== inventory

    @Override
    public List<ReportRow> lowStock() throws SQLException {
        String sql = "SELECT m.name, m.strength, c.name AS category, m.stock_quantity, m.reorder_level "
                   + "FROM medicines m JOIN categories c ON c.id = m.category_id "
                   + "WHERE m.is_discontinued = 0 AND m.stock_quantity <= m.reorder_level "
                   + "ORDER BY m.stock_quantity, m.name";
        List<ReportRow> rows = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new ReportRow(nameWithStrength(rs), rs.getString("category"),
                        rs.getInt("stock_quantity"), null, rs.getInt("reorder_level")));
            }
        }
        return rows;
    }

    @Override
    public List<ReportRow> expiringSoon(int days) throws SQLException {
        String sql = "SELECT m.name, m.strength, m.expiry_date, m.stock_quantity FROM medicines m "
                   + "WHERE m.is_discontinued = 0 AND m.expiry_date IS NOT NULL "
                   + "AND m.expiry_date < DATEADD(day, ?, CAST(SYSDATETIME() AS DATE)) "
                   + "ORDER BY m.expiry_date, m.name";
        List<ReportRow> rows = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ReportRow(nameWithStrength(rs), rs.getDate("expiry_date").toLocalDate().toString(),
                            rs.getInt("stock_quantity"), null, 0));
                }
            }
        }
        return rows;
    }

    @Override
    public BigDecimal stockValue() throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT SUM(price * stock_quantity) FROM medicines WHERE is_discontinued = 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return money(rs.getBigDecimal(1));
        }
    }

    @Override
    public int outOfStockCount() throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM medicines WHERE is_discontinued = 0 AND stock_quantity = 0");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ============================================================ helpers

    /** Sets the two dates of the period at parameter positions index and index + 1. */
    private static void setPeriod(PreparedStatement ps, int index, ReportPeriod p) throws SQLException {
        ps.setDate(index, Date.valueOf(p.getFrom()));
        ps.setDate(index + 1, Date.valueOf(p.getToExclusive()));
    }

    private static int count(Connection con, String sql, ReportPeriod p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            setPeriod(ps, 1, p);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Runs a report query and maps each row. The query's parameters are an
     * optional TOP (?) limit first, then the period.
     */
    private List<ReportRow> rows(String sql, Integer limit, ReportPeriod p, String labelColumn,
                                 String detailColumn, String countColumn, String amountColumn,
                                 String extraColumn) throws SQLException {
        List<ReportRow> rows = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int index = 1;
            if (limit != null) {
                ps.setInt(index++, limit);
            }
            setPeriod(ps, index, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReportRow row = new ReportRow();
                    row.setLabel(rs.getString(labelColumn));
                    row.setDetail(detailColumn == null ? null : rs.getString(detailColumn));
                    row.setCount(rs.getInt(countColumn));
                    row.setAmount(amountColumn == null ? null : money(rs.getBigDecimal(amountColumn)));
                    row.setExtra(extraColumn == null ? 0 : rs.getInt(extraColumn));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private static String nameWithStrength(ResultSet rs) throws SQLException {
        String strength = rs.getString("strength");
        return rs.getString("name") + (strength == null ? "" : " " + strength);
    }

    /** SUM() gives NULL when there are no rows - show that as 0.00. */
    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
