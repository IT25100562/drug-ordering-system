package com.medisys.user;

import com.medisys.common.DBConnection;
import com.medisys.common.TextUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for the users table: every SQL query about users is in this class.
 *
 *   CREATE  addUser
 *   READ    getUserById, getUserByEmail, getUsers, countUsers, ...
 *   UPDATE  updateContactDetails, updatePassword, updatePhoto, setActive, flagCustomer, removeFlag
 *   DELETE  - users are never deleted, because their orders and prescriptions
 *             must be kept. Staff who leave are switched off (setActive).
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
public class UserDAO {

    /** Filters of the admin's users page. */
    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_CUSTOMERS = "CUSTOMERS";
    public static final String FILTER_STAFF = "STAFF";
    public static final String FILTER_FLAGGED = "FLAGGED";

    // The user, and the name of the pharmacist who flagged them (LEFT JOIN: usually nobody).
    // The password hash is never read here, so it can't end up in the session by mistake.
    private static final String SELECT_USER =
            "SELECT u.id, u.full_name, u.email, u.phone, u.whatsapp, u.nic, u.date_of_birth, u.address, "
            + "u.photo_key, u.role, u.is_active, u.is_flagged, u.flag_reason, u.flagged_at, u.created_at, "
            + "f.full_name AS flagged_by_name "
            + "FROM users u LEFT JOIN users f ON f.id = u.flagged_by ";

    // ================================================================ CREATE

    /** Saves a new user (customer or staff) and returns the new id. */
    public int addUser(User u, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, phone, whatsapp, nic, date_of_birth, address, "
                   + "password_hash, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getWhatsapp());
            ps.setString(5, u.getNic());
            ps.setDate(6, u.getDateOfBirth() == null ? null : Date.valueOf(u.getDateOfBirth()));
            ps.setString(7, u.getAddress());
            ps.setString(8, passwordHash);
            ps.setString(9, u.getRole().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // ================================================================== READ

    /** The user, or null when the id does not exist. */
    public User getUserById(int id) throws SQLException {
        List<User> list = query(SELECT_USER + "WHERE u.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    /** The user, or null when no user has this email. */
    public User getUserByEmail(String email) throws SQLException {
        List<User> list = query(SELECT_USER + "WHERE LOWER(u.email) = LOWER(?)", email);
        return list.isEmpty() ? null : list.get(0);
    }

    /** The stored password hash of a user (only used to check a password). */
    public String getPasswordHash(int id) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT password_hash FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** True when another account (not excludeId) already uses this email. */
    public boolean emailExists(String email, int excludeId) throws SQLException {
        return count("SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?) AND id <> ?", email, excludeId) > 0;
    }

    /** True when another account (not excludeId) already uses this NIC. */
    public boolean nicExists(String nic, int excludeId) throws SQLException {
        return count("SELECT COUNT(*) FROM users WHERE UPPER(nic) = UPPER(?) AND id <> ?", nic, excludeId) > 0;
    }

    /**
     * Users for the admin page: flagged first, then newest first.
     *
     * @param filter  one of the FILTER_ values
     * @param keyword part of the name or email (may be empty)
     */
    public List<User> getUsers(String filter, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_USER).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (FILTER_CUSTOMERS.equals(filter)) {
            sql.append("AND u.role = 'CUSTOMER' ");
        } else if (FILTER_STAFF.equals(filter)) {
            sql.append("AND u.role <> 'CUSTOMER' ");
        } else if (FILTER_FLAGGED.equals(filter)) {
            sql.append("AND u.is_flagged = TRUE ");
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = TextUtil.likePattern(keyword);
            sql.append("AND (u.full_name ILIKE ? OR u.email ILIKE ?) ");
            params.add(pattern);
            params.add(pattern);
        }
        sql.append("ORDER BY u.is_flagged DESC, u.created_at DESC, u.id DESC");
        return query(sql.toString(), params.toArray());
    }

    /** Number of users for each FILTER_ value (the tabs of the admin page). */
    public Map<String, Integer> countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) AS all_users, "
                   + "SUM(CASE WHEN role = 'CUSTOMER' THEN 1 ELSE 0 END) AS customers, "
                   + "SUM(CASE WHEN role <> 'CUSTOMER' THEN 1 ELSE 0 END) AS staff, "
                   + "SUM(CASE WHEN is_flagged = TRUE THEN 1 ELSE 0 END) AS flagged "
                   + "FROM users";
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            counts.put(FILTER_ALL, rs.getInt("all_users"));
            counts.put(FILTER_CUSTOMERS, rs.getInt("customers"));
            counts.put(FILTER_STAFF, rs.getInt("staff"));
            counts.put(FILTER_FLAGGED, rs.getInt("flagged"));
        }
        return counts;
    }

    /** A customer's history in numbers, for the pharmacist: "prescriptions", "rejected", "orders". */
    public Map<String, Integer> getCustomerStats(int userId) throws SQLException {
        String sql = "SELECT "
                   + "(SELECT COUNT(*) FROM prescriptions WHERE user_id = ?) AS prescriptions, "
                   + "(SELECT COUNT(*) FROM prescriptions WHERE user_id = ? AND status = 'REJECTED') AS rejected, "
                   + "(SELECT COUNT(*) FROM orders WHERE user_id = ?) AS orders";
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                stats.put("prescriptions", rs.getInt("prescriptions"));
                stats.put("rejected", rs.getInt("rejected"));
                stats.put("orders", rs.getInt("orders"));
            }
        }
        return stats;
    }

    // ================================================================ UPDATE

    /** Saves the details a user may change: name, phone, WhatsApp, address. */
    public void updateContactDetails(User u) throws SQLException {
        update("UPDATE users SET full_name = ?, phone = ?, whatsapp = ?, address = ?, updated_at = CURRENT_TIMESTAMP "
               + "WHERE id = ?", u.getFullName(), u.getPhone(), u.getWhatsapp(), u.getAddress(), u.getId());
    }

    public void updatePassword(int id, String passwordHash) throws SQLException {
        update("UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", passwordHash, id);
    }

    /** Sets the profile photo, or removes it with null. */
    public void updatePhoto(int id, String photoKey) throws SQLException {
        update("UPDATE users SET photo_key = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", photoKey, id);
    }

    /** Staff accounts only: turn login on or off. Customers are never switched off. */
    public boolean setActive(int id, boolean active) throws SQLException {
        return update("UPDATE users SET is_active = ?, updated_at = CURRENT_TIMESTAMP "
                      + "WHERE id = ? AND role <> 'CUSTOMER'", active, id) == 1;
    }

    /** Puts a red flag on a customer. */
    public boolean flagCustomer(int id, String reason, int flaggedBy) throws SQLException {
        return update("UPDATE users SET is_flagged = TRUE, flag_reason = ?, flagged_by = ?, flagged_at = CURRENT_TIMESTAMP "
                      + "WHERE id = ? AND role = 'CUSTOMER'", reason, flaggedBy, id) == 1;
    }

    /** Removes the red flag. */
    public boolean removeFlag(int id) throws SQLException {
        return update("UPDATE users SET is_flagged = FALSE, flag_reason = NULL, flagged_by = NULL, flagged_at = NULL "
                      + "WHERE id = ? AND is_flagged = TRUE", id) == 1;
    }

    // =============================================================== helpers

    /** Runs an UPDATE and returns the number of changed rows. */
    private int update(String sql, Object... params) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    private int count(String sql, Object... params) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<User> query(String sql, Object... params) throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** Turns the current row into a User object. */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setWhatsapp(rs.getString("whatsapp"));
        user.setNic(rs.getString("nic"));
        Date born = rs.getDate("date_of_birth");
        user.setDateOfBirth(born == null ? null : born.toLocalDate());
        user.setAddress(rs.getString("address"));
        user.setPhotoKey(rs.getString("photo_key"));
        user.setRole(Role.fromText(rs.getString("role")));
        user.setActive(rs.getBoolean("is_active"));
        user.setFlagged(rs.getBoolean("is_flagged"));
        user.setFlagReason(rs.getString("flag_reason"));
        user.setFlaggedByName(rs.getString("flagged_by_name"));
        Timestamp flaggedAt = rs.getTimestamp("flagged_at");
        user.setFlaggedAt(flaggedAt == null ? null : flaggedAt.toLocalDateTime());
        Timestamp created = rs.getTimestamp("created_at");
        user.setCreatedAt(created == null ? null : created.toLocalDateTime());
        return user;
    }
}
