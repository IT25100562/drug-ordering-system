package com.medisys.medicine;

import com.medisys.common.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the categories table.
 *
 *   CREATE  addCategory
 *   READ    getAllCategories, getCategoryById, categoryExists, countMedicines
 *   DELETE  deleteCategory
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
public class CategoryDAO {

    // ================================================================ CREATE

    public int addCategory(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // ================================================================== READ

    /** All categories, A to Z, each with its medicine count. */
    public List<Category> getAllCategories() throws SQLException {
        String sql = "SELECT c.id, c.name, c.description, COUNT(m.id) AS medicine_count "
                   + "FROM categories c LEFT JOIN medicines m ON m.category_id = c.id "
                   + "GROUP BY c.id, c.name, c.description "
                   + "ORDER BY c.name";
        List<Category> categories = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category category = new Category(rs.getInt("id"), rs.getString("name"), rs.getString("description"));
                category.setMedicineCount(rs.getInt("medicine_count"));
                categories.add(category);
            }
        }
        return categories;
    }

    /** The category, or null when the id does not exist. */
    public Category getCategoryById(int id) throws SQLException {
        String sql = "SELECT id, name, description FROM categories WHERE id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Category(rs.getInt("id"), rs.getString("name"), rs.getString("description")) : null;
            }
        }
    }

    public boolean categoryExists(String name) throws SQLException {
        return count("SELECT COUNT(*) FROM categories WHERE name = ?", name) > 0;
    }

    /** How many medicines (including discontinued ones) use this category. */
    public int countMedicines(int categoryId) throws SQLException {
        return count("SELECT COUNT(*) FROM medicines WHERE category_id = ?", categoryId);
    }

    // ================================================================ DELETE

    public boolean deleteCategory(int id) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    // =============================================================== helpers

    private int count(String sql, Object value) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
