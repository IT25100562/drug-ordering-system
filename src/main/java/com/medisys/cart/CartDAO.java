package com.medisys.cart;

import com.medisys.common.DBConnection;
import com.medisys.medicine.MedicineDAO;

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
 * DAO for the cart_items table. A customer has at most one line per
 * medicine, so a line is found by (userId, medicineId).
 *
 *   CREATE  addItem
 *   READ    getCart, getQuantity, getQuantities, countItems
 *   UPDATE  updateQuantity
 *   DELETE  removeItem, clearCart
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public class CartDAO {

    // ================================================================ CREATE

    public void addItem(int userId, int medicineId, int quantity) throws SQLException {
        String sql = "INSERT INTO cart_items (user_id, medicine_id, quantity) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, medicineId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        }
    }

    // ================================================================== READ

    /** The customer's cart, newest line first. Each line has the medicine's latest price and stock. */
    public Cart getCart(int userId) throws SQLException {
        // The cart columns get their own names so they do not clash with m.id etc.
        String sql = MedicineDAO.SELECT_MEDICINE.replace("SELECT ",
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
                    item.setMedicine(MedicineDAO.mapRow(rs));
                    items.add(item);
                }
            }
        }
        return new Cart(items);
    }

    /** The quantity of this medicine in the cart, or 0 when it is not there. */
    public int getQuantity(int userId, int medicineId) throws SQLException {
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

    /** medicineId -> quantity, for showing "2 in cart" on the catalog. */
    public Map<Integer, Integer> getQuantities(int userId) throws SQLException {
        Map<Integer, Integer> quantities = new HashMap<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT medicine_id, quantity FROM cart_items WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    quantities.put(rs.getInt("medicine_id"), rs.getInt("quantity"));
                }
            }
        }
        return quantities;
    }

    /** Total number of packs in the cart (for the badge in the menu). */
    public int countItems(int userId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT ISNULL(SUM(quantity), 0) FROM cart_items WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ================================================================ UPDATE

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

    // ================================================================ DELETE

    public boolean removeItem(int userId, int medicineId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM cart_items WHERE user_id = ? AND medicine_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, medicineId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Empties the cart. */
    public void clearCart(int userId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM cart_items WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}
