package com.medisys.cart;

import com.medisys.common.DBConnection;
import com.medisys.medicine.MedicineDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DAO for the wishlist_items table.
 *
 *   CREATE  addItem
 *   READ    getWishlist, getMedicineIds, isSaved, countItems
 *   DELETE  removeItem
 *   (a saved medicine has nothing to update)
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public class WishlistDAO {

    // ================================================================ CREATE

    public void addItem(int userId, int medicineId) throws SQLException {
        // Insert only when it is not saved yet, so a double click is harmless.
        String sql = "INSERT INTO wishlist_items (user_id, medicine_id) "
                   + "SELECT ?, ? WHERE NOT EXISTS "
                   + "(SELECT 1 FROM wishlist_items WHERE user_id = ? AND medicine_id = ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, medicineId);
            ps.setInt(3, userId);
            ps.setInt(4, medicineId);
            ps.executeUpdate();
        }
    }

    // ================================================================== READ

    /** The customer's saved medicines, newest first. */
    public List<WishlistItem> getWishlist(int userId) throws SQLException {
        String sql = MedicineDAO.SELECT_MEDICINE.replace("SELECT ",
                "SELECT w.id AS wishlist_item_id, w.added_at AS wishlist_added_at, ")
                + "JOIN wishlist_items w ON w.medicine_id = m.id "
                + "WHERE w.user_id = ? "
                + "ORDER BY w.added_at DESC, w.id DESC";

        List<WishlistItem> items = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WishlistItem item = new WishlistItem();
                    item.setId(rs.getInt("wishlist_item_id"));
                    item.setUserId(userId);
                    Timestamp added = rs.getTimestamp("wishlist_added_at");
                    item.setAddedAt(added == null ? null : added.toLocalDateTime());
                    item.setMedicine(MedicineDAO.mapRow(rs));
                    items.add(item);
                }
            }
        }
        return items;
    }

    /** Ids of the saved medicines, for the filled hearts on the catalog. */
    public Set<Integer> getMedicineIds(int userId) throws SQLException {
        Set<Integer> ids = new HashSet<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT medicine_id FROM wishlist_items WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    public boolean isSaved(int userId, int medicineId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT COUNT(*) FROM wishlist_items WHERE user_id = ? AND medicine_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public int countItems(int userId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM wishlist_items WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ================================================================ DELETE

    public boolean removeItem(int userId, int medicineId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM wishlist_items WHERE user_id = ? AND medicine_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, medicineId);
            return ps.executeUpdate() == 1;
        }
    }
}
