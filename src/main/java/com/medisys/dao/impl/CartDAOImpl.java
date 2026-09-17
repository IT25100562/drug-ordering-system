package com.medisys.dao.impl;

import com.medisys.config.DBConnection;
import com.medisys.dao.CartDAO;
import com.medisys.model.CartItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of CartDAO (SQL Server).
 *
 * The medicine columns are read with MedicineDAOImpl.mapRow, so a cart line
 * always has the latest price and stock.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public class CartDAOImpl implements CartDAO {

    @Override
    public List<CartItem> findByUser(int userId) throws SQLException {
        // Cart columns get their own names so they do not clash with m.id etc.
        String sql = MedicineDAOImpl.SELECT_MEDICINE.replace("SELECT ",
                "SELECT ci.id AS cart_item_id, ci.quantity AS cart_quantity, ci.added_at AS cart_added_at, ")
                + "JOIN cart_items ci ON ci.medicine_id = m.id "
                + "WHERE ci.user_id = ? "
                + "ORDER BY ci.added_at DESC, ci.id DESC";

        List<CartItem> items = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CartItem item = new CartItem();
                    item.setId(rs.getInt("cart_item_id"));
                    item.setUserId(userId);
                    item.setQuantity(rs.getInt("cart_quantity"));
                    Timestamp added = rs.getTimestamp("cart_added_at");
                    item.setAddedAt(added == null ? null : added.toLocalDateTime());
                    item.setMedicine(MedicineDAOImpl.mapRow(rs));
                    items.add(item);
                }
            }
        }
        return items;
    }

    @Override
    public int findQuantity(int userId, int medicineId) throws SQLException {
        String sql = "SELECT quantity FROM cart_items WHERE user_id = ? AND medicine_id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public Map<Integer, Integer> findQuantities(int userId) throws SQLException {
        String sql = "SELECT medicine_id, quantity FROM cart_items WHERE user_id = ?";
        Map<Integer, Integer> quantities = new HashMap<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    quantities.put(rs.getInt("medicine_id"), rs.getInt("quantity"));
                }
            }
        }
        return quantities;
    }

    @Override
    public void add(int userId, int medicineId, int quantity) throws SQLException {
        String sql = "INSERT INTO cart_items (user_id, medicine_id, quantity) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, medicineId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean updateQuantity(int userId, int medicineId, int quantity) throws SQLException {
        String sql = "UPDATE cart_items SET quantity = ?, updated_at = SYSDATETIME() "
                   + "WHERE user_id = ? AND medicine_id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, userId);
            ps.setInt(3, medicineId);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean remove(int userId, int medicineId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ? AND medicine_id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, medicineId);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public void clear(int userId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM cart_items WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    @Override
    public int countItems(int userId) throws SQLException {
        String sql = "SELECT ISNULL(SUM(quantity), 0) FROM cart_items WHERE user_id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
