package com.medisys.dao.impl;

import com.medisys.config.DBConnection;
import com.medisys.dao.NotificationDAO;
import com.medisys.model.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of NotificationDAO (SQL Server).
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
public class NotificationDAOImpl implements NotificationDAO {

    @Override
    public void create(int userId, String message, String link) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, message, link) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.setString(3, link);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Notification> findByUser(int userId, int limit) throws SQLException {
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

    @Override
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

    @Override
    public void markAllRead(int userId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean delete(int id, int userId) throws SQLException {
        // "AND user_id = ?" makes sure nobody can delete someone else's notification.
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM notifications WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public int deleteRead(int userId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM notifications WHERE user_id = ? AND is_read = 1")) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        }
    }
}
