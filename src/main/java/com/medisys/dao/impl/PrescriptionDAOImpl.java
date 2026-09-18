package com.medisys.dao.impl;

import com.medisys.config.DBConnection;
import com.medisys.dao.PrescriptionDAO;
import com.medisys.model.Prescription;
import com.medisys.model.PrescriptionItem;
import com.medisys.model.PrescriptionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of PrescriptionDAO (SQL Server).
 *
 * A prescription is read with its customer and reviewer (one query), then its
 * medicine lines are read for all prescriptions of the list at once (a second
 * query), using MedicineDAOImpl.mapRow for the medicines. The payment details
 * come from the order that paid for the prescription (prescriptions.order_id).
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
public class PrescriptionDAOImpl implements PrescriptionDAO {

    private static final String SELECT_PRESCRIPTION =
            "SELECT p.*, u.full_name AS customer_name, u.email AS customer_email, "
            + "u.phone AS customer_phone, u.address AS customer_address, r.full_name AS reviewer_name, "
            // the red flag and photo (user accounts), for the pharmacist's list
            + "u.is_flagged AS customer_flagged, u.photo_key AS customer_photo_key, "
            // the payment comes from the order that paid for it (module 02)
            + "o.created_at AS paid_at, o.total AS amount_paid, o.status AS order_status, "
            + "o.delivery_name, o.delivery_address, o.delivery_phone, "
            + "pay.reference AS payment_reference, pay.card_last4 "
            + "FROM prescriptions p "
            + "JOIN users u ON u.id = p.user_id "
            + "LEFT JOIN users r ON r.id = p.reviewed_by "
            + "LEFT JOIN orders o ON o.id = p.order_id "
            + "LEFT JOIN payments pay ON pay.order_id = o.id ";

    private static final String SELECT_ITEMS = MedicineDAOImpl.SELECT_MEDICINE.replace("SELECT ",
            "SELECT i.id AS item_id, i.prescription_id AS item_rx_id, i.quantity AS item_quantity, "
            + "i.dosage_instructions AS item_dosage, i.unit_price AS item_unit_price, ")
            + "JOIN prescription_items i ON i.medicine_id = m.id ";

    /** SQL condition: uploaded more than 30 days ago and not paid. */
    private static final String EXPIRED =
            "(p.order_id IS NULL AND p.uploaded_at < DATEADD(day, -" + Prescription.EXPIRY_DAYS + ", SYSDATETIME()))";

    @Override
    public int create(Prescription p) throws SQLException {
        String sql = "INSERT INTO prescriptions (user_id, customer_note, file_key, original_file_name, "
                   + "content_type, file_size) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getUserId());
            ps.setString(2, p.getCustomerNote());
            ps.setString(3, p.getFileKey());
            ps.setString(4, p.getOriginalFileName());
            ps.setString(5, p.getContentType());
            ps.setInt(6, p.getFileSize());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public Prescription findById(int id) throws SQLException {
        List<Prescription> list = query(SELECT_PRESCRIPTION + "WHERE p.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Prescription> findByUser(int userId) throws SQLException {
        return query(SELECT_PRESCRIPTION + "WHERE p.user_id = ? ORDER BY p.updated_at DESC, p.id DESC", userId);
    }

    @Override
    public List<Prescription> findForDashboard(String filter) throws SQLException {
        String order = "ORDER BY p.updated_at DESC, p.id DESC";
        String where;
        switch (filter) {
            case "PENDING":
                // The waiting queue: oldest first, so nobody waits too long.
                where = "WHERE p.status = 'PENDING' ";
                order = "ORDER BY p.uploaded_at ASC, p.id ASC";
                break;
            case "CORRECTION_REQUESTED":
                where = "WHERE p.status = 'CORRECTION_REQUESTED' ";
                break;
            case "REJECTED":
                where = "WHERE p.status = 'REJECTED' ";
                break;
            case FILTER_APPROVED:
                where = "WHERE p.status = 'APPROVED' AND p.order_id IS NULL ";
                break;
            case FILTER_PAID:
                where = "WHERE p.order_id IS NOT NULL ";
                order = "ORDER BY o.created_at DESC";
                break;
            case FILTER_EXPIRED:
                where = "WHERE " + EXPIRED + " ";
                break;
            case FILTER_ALL:
                where = "";
                break;
            default:
                return new ArrayList<>();
        }
        return query(SELECT_PRESCRIPTION + where + order);
    }

    @Override
    public Map<String, Integer> countForDashboard() throws SQLException {
        String sql = "SELECT "
                + "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN status = 'CORRECTION_REQUESTED' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN status = 'APPROVED' AND order_id IS NULL THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN order_id IS NOT NULL THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN " + EXPIRED + " THEN 1 ELSE 0 END), "
                + "COUNT(*) "
                + "FROM prescriptions p";
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            counts.put("PENDING", rs.getInt(1));
            counts.put("CORRECTION_REQUESTED", rs.getInt(2));
            counts.put(FILTER_APPROVED, rs.getInt(3));
            counts.put(FILTER_PAID, rs.getInt(4));
            counts.put("REJECTED", rs.getInt(5));
            counts.put(FILTER_EXPIRED, rs.getInt(6));
            counts.put(FILTER_ALL, rs.getInt(7));
        }
        return counts;
    }

    @Override
    public int countOpen(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM prescriptions p WHERE p.user_id = ? "
                   + "AND p.status IN ('PENDING', 'CORRECTION_REQUESTED') AND NOT " + EXPIRED;
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public boolean approve(int id, String note, int reviewerId, List<PrescriptionItem> items) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                // "AND status = 'PENDING'" makes sure two pharmacists cannot both decide.
                String update = "UPDATE prescriptions SET status = 'APPROVED', pharmacist_note = ?, "
                              + "reviewed_by = ?, reviewed_at = SYSDATETIME(), updated_at = SYSDATETIME() "
                              + "WHERE id = ? AND status = 'PENDING'";
                try (PreparedStatement ps = con.prepareStatement(update)) {
                    ps.setString(1, note);
                    ps.setInt(2, reviewerId);
                    ps.setInt(3, id);
                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }
                String insert = "INSERT INTO prescription_items "
                              + "(prescription_id, medicine_id, quantity, dosage_instructions, unit_price) "
                              + "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(insert)) {
                    for (PrescriptionItem item : items) {
                        ps.setInt(1, id);
                        ps.setInt(2, item.getMedicine().getId());
                        ps.setInt(3, item.getQuantity());
                        ps.setString(4, item.getDosageInstructions());
                        ps.setBigDecimal(5, item.getUnitPrice());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    @Override
    public boolean decide(int id, PrescriptionStatus status, String note, int reviewerId) throws SQLException {
        String sql = "UPDATE prescriptions SET status = ?, pharmacist_note = ?, reviewed_by = ?, "
                   + "reviewed_at = SYSDATETIME(), updated_at = SYSDATETIME() "
                   + "WHERE id = ? AND status = 'PENDING'";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, note);
            ps.setInt(3, reviewerId);
            ps.setInt(4, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean replaceFile(int id, String fileKey, String originalFileName, String contentType, int fileSize)
            throws SQLException {
        // The new copy is a new document, so its 30 days start again (uploaded_at).
        // The pharmacist's last note is kept so they can see what was asked.
        String sql = "UPDATE prescriptions SET file_key = ?, original_file_name = ?, content_type = ?, "
                   + "file_size = ?, status = 'PENDING', correction_count = correction_count + 1, "
                   + "uploaded_at = SYSDATETIME(), updated_at = SYSDATETIME() "
                   + "WHERE id = ? AND status = 'CORRECTION_REQUESTED'";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fileKey);
            ps.setString(2, originalFileName);
            ps.setString(3, contentType);
            ps.setInt(4, fileSize);
            ps.setInt(5, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        // The medicine lines go too (ON DELETE CASCADE).
        String sql = "DELETE FROM prescriptions WHERE id = ? AND order_id IS NULL";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    // ------------------------------------------------------------ helpers

    /** Runs a prescription query, then loads the medicine lines of all results. */
    private List<Prescription> query(String sql, Object... params) throws SQLException {
        List<Prescription> list = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
            loadItems(con, list);
        }
        return list;
    }

    private void loadItems(Connection con, List<Prescription> list) throws SQLException {
        if (list.isEmpty()) {
            return;
        }
        Map<Integer, Prescription> byId = new HashMap<>();
        for (Prescription p : list) {
            byId.put(p.getId(), p);
        }
        // One "?" per prescription: WHERE i.prescription_id IN (?, ?, ?)
        String marks = String.join(", ", Collections.nCopies(list.size(), "?"));
        String sql = SELECT_ITEMS + "WHERE i.prescription_id IN (" + marks + ") ORDER BY i.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < list.size(); i++) {
                ps.setInt(i + 1, list.get(i).getId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrescriptionItem item = new PrescriptionItem();
                    item.setId(rs.getInt("item_id"));
                    item.setQuantity(rs.getInt("item_quantity"));
                    item.setDosageInstructions(rs.getString("item_dosage"));
                    item.setUnitPrice(rs.getBigDecimal("item_unit_price"));
                    item.setMedicine(MedicineDAOImpl.mapRow(rs));
                    byId.get(rs.getInt("item_rx_id")).getItems().add(item);
                }
            }
        }
    }

    private Prescription mapRow(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setId(rs.getInt("id"));
        p.setUserId(rs.getInt("user_id"));
        p.setCustomerName(rs.getString("customer_name"));
        p.setCustomerEmail(rs.getString("customer_email"));
        p.setCustomerPhone(rs.getString("customer_phone"));
        p.setCustomerAddress(rs.getString("customer_address"));
        p.setCustomerFlagged(rs.getBoolean("customer_flagged"));
        p.setCustomerHasPhoto(rs.getString("customer_photo_key") != null);
        p.setCustomerNote(rs.getString("customer_note"));
        p.setFileKey(rs.getString("file_key"));
        p.setOriginalFileName(rs.getString("original_file_name"));
        p.setContentType(rs.getString("content_type"));
        p.setFileSize(rs.getInt("file_size"));
        p.setStatus(PrescriptionStatus.fromText(rs.getString("status")));
        p.setPharmacistNote(rs.getString("pharmacist_note"));
        p.setReviewedById((Integer) rs.getObject("reviewed_by"));
        p.setReviewedByName(rs.getString("reviewer_name"));
        p.setReviewedAt(toTime(rs.getTimestamp("reviewed_at")));
        p.setCorrectionCount(rs.getInt("correction_count"));
        p.setUploadedAt(toTime(rs.getTimestamp("uploaded_at")));
        p.setUpdatedAt(toTime(rs.getTimestamp("updated_at")));
        p.setOrderId((Integer) rs.getObject("order_id"));
        p.setOrderStatus(rs.getString("order_status"));
        p.setPaidAt(toTime(rs.getTimestamp("paid_at")));
        p.setAmountPaid(rs.getBigDecimal("amount_paid"));
        p.setPaymentReference(rs.getString("payment_reference"));
        p.setCardLast4(rs.getString("card_last4"));
        p.setDeliveryName(rs.getString("delivery_name"));
        p.setDeliveryAddress(rs.getString("delivery_address"));
        p.setDeliveryPhone(rs.getString("delivery_phone"));
        return p;
    }

    private static LocalDateTime toTime(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
