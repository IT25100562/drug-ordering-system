package com.medisys.dao.impl;

import com.medisys.config.DBConnection;
import com.medisys.dao.CategoryDAO;
import com.medisys.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of CategoryDAO (SQL Server).
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
public class CategoryDAOImpl implements CategoryDAO {

    @Override
    public List<Category> findAll() throws SQLException {
        String sql = "SELECT c.id, c.name, c.description, COUNT(m.id) AS medicine_count "
                   + "FROM categories c LEFT JOIN medicines m ON m.category_id = c.id "
                   + "GROUP BY c.id, c.name, c.description "
                   + "ORDER BY c.name";
        List<Category> categories = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category category = new Category(rs.getInt("id"), rs.getString("name"),
                        rs.getString("description"));
                category.setMedicineCount(rs.getInt("medicine_count"));
                categories.add(category);
            }
        }
        return categories;
    }

    @Override
    public Category findById(int id) throws SQLException {
        String sql = "SELECT id, name, description FROM categories WHERE id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Category(rs.getInt("id"), rs.getString("name"), rs.getString("description"));
            }
        }
    }

    @Override
    public boolean existsByName(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories WHERE name = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public int create(Category category) throws SQLException {
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

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public int countMedicines(int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM medicines WHERE category_id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
