package com.medisys.delivery;

import com.medisys.common.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the notifications table. Every module tells its users what happened
 * by calling addNotification().
 *
 *   CREATE  addNotification
 *   READ    getNotifications, countUnread
 *   UPDATE  markAllRead
 *   DELETE  deleteNotification, deleteReadNotifications
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
public class NotificationDAO {

    private static final int MESSAGE_MAX = 500;

    // ================================================================ CREATE

    /**
     * Sends a message to a user.
     *
     * A failed notification must not undo the action that caused it (for
     * example the pharmacist's decision is already saved), so a database
     * error is only written to the log.
     *
     * @param link a page inside the app such as "/prescriptions", or null
     */
    public void addNotification(int userId, String message, String link) {
        String text = message.length() > MESSAGE_MAX ? message.substring(0, MESSAGE_MAX - 3) + "..." : message;
        String sql = "INSERT INTO notifications (user_id, message, link) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, text);
            ps.setString(3, link);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[MediSys] Could not save a notification for user " + userId + ": " + e.getMessage());
        }
    }

    // ================================================================== READ

    /** The user's newest notifications (at most "limit"). */
    public List<Notification> getNotifications(int userId, int limit) throws SQLException {
        String sql = "SELECT TOP (?) id, user_id, message, link, is_read, created_at FROM notifications "
                   + "WHERE user_id = ? ORDER BY created_at DESC, id DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setId(rs.getInt("id"));
                    n.setUserId(rs.getInt("user_id"));
                    n.setMessage(rs.getString("message"));
                    n.setLink(rs.getString("link"));
                    n.setRead(rs.getBoolean("is_read"));
                    Timestamp created = rs.getTimestamp("created_at");
                    n.setCreatedAt(created == null ? null : created.toLocalDateTime());
                    list.add(n);
                }
            }
        }
        return list;
    }

    public int countUnread(int userId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ================================================================ UPDATE

    public void markAllRead(int userId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // ================================================================ DELETE

    /** Deletes one of the user's notifications. False if it is not theirs (or already gone). */
    public boolean deleteNotification(int id, int userId) throws SQLException {
        // "AND user_id = ?" makes sure nobody can delete someone else's notification.
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM notifications WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Deletes all of the user's read notifications. Returns how many were deleted. */
    public int deleteReadNotifications(int userId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM notifications WHERE user_id = ? AND is_read = 1")) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        }
    }
}
