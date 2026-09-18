package com.medisys.dao.impl;

import com.medisys.config.DBConnection;
import com.medisys.dao.SavedReportDAO;
import com.medisys.model.SavedReport;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of SavedReportDAO (SQL Server).
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
public class SavedReportDAOImpl implements SavedReportDAO {

    private static final String SELECT =
            "SELECT r.*, u.full_name AS created_by_name FROM saved_reports r "
            + "JOIN users u ON u.id = r.created_by ";

    @Override
    public int create(SavedReport r) throws SQLException {
        String sql = "INSERT INTO saved_reports (title, notes, period_from, period_to, revenue, order_count, "
                   + "average_order, cancelled_count, refunded_amount, prescription_count, approval_rate, "
                   + "delivered_count, on_time_rate, new_customers, created_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getTitle());
            ps.setString(2, r.getNotes());
            ps.setDate(3, Date.valueOf(r.getPeriodFrom()));
            ps.setDate(4, Date.valueOf(r.getPeriodTo()));
            ps.setBigDecimal(5, r.getRevenue());
            ps.setInt(6, r.getOrderCount());
            ps.setBigDecimal(7, r.getAverageOrder());
            ps.setInt(8, r.getCancelledCount());
            ps.setBigDecimal(9, r.getRefundedAmount());
            ps.setInt(10, r.getPrescriptionCount());
            ps.setBigDecimal(11, r.getApprovalRate());
            ps.setInt(12, r.getDeliveredCount());
            ps.setBigDecimal(13, r.getOnTimeRate());
            ps.setInt(14, r.getNewCustomers());
            ps.setInt(15, r.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public SavedReport findById(int id) throws SQLException {
        List<SavedReport> list = query(SELECT + "WHERE r.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<SavedReport> findAll() throws SQLException {
        return query(SELECT + "ORDER BY r.period_to DESC, r.id DESC");
    }

    @Override
    public boolean update(int id, String title, String notes) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE saved_reports SET title = ?, notes = ?, updated_at = SYSDATETIME() WHERE id = ?")) {
            ps.setString(1, title);
            ps.setString(2, notes);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM saved_reports WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private List<SavedReport> query(String sql, Object... params) throws SQLException {
        List<SavedReport> list = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SavedReport r = new SavedReport();
                    r.setId(rs.getInt("id"));
                    r.setTitle(rs.getString("title"));
                    r.setNotes(rs.getString("notes"));
                    r.setPeriodFrom(rs.getDate("period_from").toLocalDate());
                    r.setPeriodTo(rs.getDate("period_to").toLocalDate());
                    r.setRevenue(rs.getBigDecimal("revenue"));
                    r.setOrderCount(rs.getInt("order_count"));
                    r.setAverageOrder(rs.getBigDecimal("average_order"));
                    r.setCancelledCount(rs.getInt("cancelled_count"));
                    r.setRefundedAmount(rs.getBigDecimal("refunded_amount"));
                    r.setPrescriptionCount(rs.getInt("prescription_count"));
                    r.setApprovalRate(rs.getBigDecimal("approval_rate"));
                    r.setDeliveredCount(rs.getInt("delivered_count"));
                    r.setOnTimeRate(rs.getBigDecimal("on_time_rate"));
                    r.setNewCustomers(rs.getInt("new_customers"));
                    r.setCreatedBy(rs.getInt("created_by"));
                    r.setCreatedByName(rs.getString("created_by_name"));
                    r.setCreatedAt(toTime(rs.getTimestamp("created_at")));
                    r.setUpdatedAt(toTime(rs.getTimestamp("updated_at")));
                    list.add(r);
                }
            }
        }
        return list;
    }

    private static LocalDateTime toTime(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
