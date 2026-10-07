package com.medisys.dao.impl;

import com.medisys.config.DBConnection;
import com.medisys.dao.UserDAO;
import com.medisys.model.Role;
import com.medisys.model.User;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of UserDAO (SQL Server).
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 */
public class UserDAOImpl implements UserDAO {

    // The user, and the name of the pharmacist who flagged them (LEFT JOIN: usually nobody).
    // The password hash is never read here, so it can't end up in the session by mistake.
    private static final String SELECT_USER =
            "SELECT u.id, u.full_name, u.email, u.phone, u.whatsapp, u.nic, u.date_of_birth, u.address, "
            + "u.photo_key, u.role, u.is_active, u.is_flagged, u.flag_reason, u.flagged_at, u.created_at, "
            + "f.full_name AS flagged_by_name "
            + "FROM users u LEFT JOIN users f ON f.id = u.flagged_by ";

    // ================================================================= read

    @Override
    public User findById(int id) throws SQLException {
        List<User> list = query(SELECT_USER + "WHERE u.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public User findByEmail(String email) throws SQLException {
        List<User> list = query(SELECT_USER + "WHERE u.email = ?", email);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public String findPasswordHash(int id) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT password_hash FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    @Override
    public boolean emailTaken(String email, int excludeId) throws SQLException {
        return exists("SELECT COUNT(*) FROM users WHERE email = ? AND id <> ?", email, excludeId);
    }

    @Override
    public boolean nicTaken(String nic, int excludeId) throws SQLException {
        return exists("SELECT COUNT(*) FROM users WHERE nic = ? AND id <> ?", nic, excludeId);
    }

    private boolean exists(String sql, Object... params) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public List<User> findForAdmin(String filter, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_USER).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (FILTER_CUSTOMERS.equals(filter)) {
            sql.append("AND u.role = 'CUSTOMER' ");
        } else if (FILTER_STAFF.equals(filter)) {
            sql.append("AND u.role <> 'CUSTOMER' ");
        } else if (FILTER_FLAGGED.equals(filter)) {
            sql.append("AND u.is_flagged = 1 ");
        }

        if (keyword != null && !keyword.isBlank()) {
            // [ % _ have a meaning in LIKE, so they are escaped.
            String pattern = "%" + keyword.trim().replace("[", "[[]").replace("%", "[%]").replace("_", "[_]") + "%";
            sql.append("AND (u.full_name LIKE ? OR u.email LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
        }
        sql.append("ORDER BY u.is_flagged DESC, u.created_at DESC, u.id DESC");
        return query(sql.toString(), params.toArray());
    }

    @Override
    public Map<String, Integer> countForAdmin() throws SQLException {
        String sql = "SELECT COUNT(*) AS all_users, "
                   + "SUM(CASE WHEN role = 'CUSTOMER' THEN 1 ELSE 0 END) AS customers, "
                   + "SUM(CASE WHEN role <> 'CUSTOMER' THEN 1 ELSE 0 END) AS staff, "
                   + "SUM(CASE WHEN is_flagged = 1 THEN 1 ELSE 0 END) AS flagged "
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

    @Override
    public Map<String, Integer> customerStats(int userId) throws SQLException {
        // Three small counts in one query (sub-queries).
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

    // ======================================================== create / update

    @Override
    public int create(User u, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, phone, whatsapp, nic, date_of_birth, address, "
                   + "password_hash, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
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

    @Override
    public void updateContact(User u) throws SQLException {
        update("UPDATE users SET full_name = ?, phone = ?, whatsapp = ?, address = ?, updated_at = SYSDATETIME() "
               + "WHERE id = ?", u.getFullName(), u.getPhone(), u.getWhatsapp(), u.getAddress(), u.getId());
    }

    @Override
    public void updatePassword(int id, String passwordHash) throws SQLException {
        update("UPDATE users SET password_hash = ?, updated_at = SYSDATETIME() WHERE id = ?", passwordHash, id);
    }

    @Override
    public void updatePhoto(int id, String photoKey) throws SQLException {
        update("UPDATE users SET photo_key = ?, updated_at = SYSDATETIME() WHERE id = ?", photoKey, id);
    }

    @Override
    public boolean setActive(int id, boolean active) throws SQLException {
        // Customers are never switched off - only staff accounts (see UserService).
        return update("UPDATE users SET is_active = ?, updated_at = SYSDATETIME() "
                      + "WHERE id = ? AND role <> 'CUSTOMER'", active, id) == 1;
    }

    @Override
    public boolean flag(int id, String reason, int flaggedBy) throws SQLException {
        return update("UPDATE users SET is_flagged = 1, flag_reason = ?, flagged_by = ?, flagged_at = SYSDATETIME() "
                      + "WHERE id = ? AND role = 'CUSTOMER'", reason, flaggedBy, id) == 1;
    }

    @Override
    public boolean clearFlag(int id) throws SQLException {
        return update("UPDATE users SET is_flagged = 0, flag_reason = NULL, flagged_by = NULL, flagged_at = NULL "
                      + "WHERE id = ? AND is_flagged = 1", id) == 1;
    }

    // ============================================================= helpers

    /** Runs an INSERT / UPDATE / DELETE and returns the number of changed rows. */
    private int update(String sql, Object... params) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
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
        user.setFlaggedAt(toTime(rs.getTimestamp("flagged_at")));
        user.setCreatedAt(toTime(rs.getTimestamp("created_at")));
        return user;
    }

    private static LocalDateTime toTime(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
