package com.medisys.dao.impl;

import com.medisys.config.DBConnection;
import com.medisys.dao.MedicineDAO;
import com.medisys.model.InventorySummary;
import com.medisys.model.Medicine;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of MedicineDAO (SQL Server).
 *
 * Every value from the user goes in through a PreparedStatement "?" so SQL
 * injection is not possible.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
public class MedicineDAOImpl implements MedicineDAO {

    /** Columns + category name, shared by every SELECT below (and by CartDAOImpl / WishlistDAOImpl). */
    static final String SELECT_MEDICINE =
            "SELECT m.id, m.name, m.category_id, c.name AS category_name, m.manufacturer, "
            + "m.dosage_form, m.strength, m.description, m.price, m.stock_quantity, "
            + "m.reorder_level, m.requires_prescription, m.expiry_date, m.is_discontinued, "
            + "m.created_at, m.updated_at "
            + "FROM medicines m JOIN categories c ON c.id = m.category_id ";

    @Override
    public List<Medicine> search(String keyword, Integer categoryId, String filter) throws SQLException {
        // Build the WHERE part from fixed pieces of SQL only; the user's values
        // are always passed as parameters.
        StringBuilder sql = new StringBuilder(SELECT_MEDICINE).append("WHERE ");
        List<Object> params = new ArrayList<>();

        if (FILTER_DISCONTINUED.equals(filter)) {
            sql.append("m.is_discontinued = 1 ");
        } else {
            sql.append("m.is_discontinued = 0 ");
            if (FILTER_CATALOG.equals(filter)) {
                sql.append("AND (m.expiry_date IS NULL OR m.expiry_date >= CAST(GETDATE() AS DATE)) ");
            } else if (FILTER_LOW_STOCK.equals(filter)) {
                sql.append("AND m.stock_quantity > 0 AND m.stock_quantity <= m.reorder_level ");
            } else if (FILTER_OUT_OF_STOCK.equals(filter)) {
                sql.append("AND m.stock_quantity = 0 ");
            } else if (FILTER_EXPIRED.equals(filter)) {
                sql.append("AND m.expiry_date < CAST(GETDATE() AS DATE) ");
            }
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (m.name LIKE ? OR m.manufacturer LIKE ?) ");
            String pattern = "%" + escapeLike(keyword.trim()) + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (categoryId != null) {
            sql.append("AND m.category_id = ? ");
            params.add(categoryId);
        }
        sql.append("ORDER BY m.name, m.strength");

        List<Medicine> medicines = new ArrayList<>();
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    medicines.add(mapRow(rs));
                }
            }
        }
        return medicines;
    }

    @Override
    public Medicine findById(int id) throws SQLException {
        String sql = SELECT_MEDICINE + "WHERE m.id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public boolean existsByNameAndStrength(String name, String strength, int excludeId) throws SQLException {
        // ISNULL lets a missing strength match another missing strength.
        String sql = "SELECT COUNT(*) FROM medicines "
                   + "WHERE name = ? AND ISNULL(strength, '') = ? AND id <> ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, strength == null ? "" : strength);
            ps.setInt(3, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public int create(Medicine m) throws SQLException {
        String sql = "INSERT INTO medicines (name, category_id, manufacturer, dosage_form, strength, "
                   + "description, price, stock_quantity, reorder_level, requires_prescription, expiry_date) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillCommonFields(ps, m);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public boolean update(Medicine m) throws SQLException {
        String sql = "UPDATE medicines SET name = ?, category_id = ?, manufacturer = ?, dosage_form = ?, "
                   + "strength = ?, description = ?, price = ?, stock_quantity = ?, reorder_level = ?, "
                   + "requires_prescription = ?, expiry_date = ?, updated_at = SYSDATETIME() "
                   + "WHERE id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            fillCommonFields(ps, m);
            ps.setInt(12, m.getId());
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean addStock(int id, int quantity) throws SQLException {
        String sql = "UPDATE medicines SET stock_quantity = stock_quantity + ?, "
                   + "updated_at = SYSDATETIME() WHERE id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean reduceStock(int id, int quantity) throws SQLException {
        // The "stock_quantity >= ?" check and the update happen in one
        // statement, so two orders at the same time cannot both take the last item.
        String sql = "UPDATE medicines SET stock_quantity = stock_quantity - ?, "
                   + "updated_at = SYSDATETIME() WHERE id = ? AND stock_quantity >= ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, id);
            ps.setInt(3, quantity);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean setDiscontinued(int id, boolean discontinued) throws SQLException {
        String sql = "UPDATE medicines SET is_discontinued = ?, updated_at = SYSDATETIME() WHERE id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, discontinued);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public InventorySummary getSummary() throws SQLException {
        String sql = "SELECT "
                + "SUM(CASE WHEN is_discontinued = 0 THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN is_discontinued = 0 AND stock_quantity > 0 "
                + "         AND stock_quantity <= reorder_level THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN is_discontinued = 0 AND stock_quantity = 0 THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN is_discontinued = 0 "
                + "         AND expiry_date < CAST(GETDATE() AS DATE) THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN is_discontinued = 1 THEN 1 ELSE 0 END) "
                + "FROM medicines";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            InventorySummary summary = new InventorySummary();
            if (rs.next()) {
                // SUM of no rows is NULL, which getInt() turns into 0.
                summary.setActiveCount(rs.getInt(1));
                summary.setLowStockCount(rs.getInt(2));
                summary.setOutOfStockCount(rs.getInt(3));
                summary.setExpiredCount(rs.getInt(4));
                summary.setDiscontinuedCount(rs.getInt(5));
            }
            return summary;
        }
    }

    // ------------------------------------------------------------ helpers

    /** Sets parameters 1-11, which INSERT and UPDATE have in the same order. */
    private void fillCommonFields(PreparedStatement ps, Medicine m) throws SQLException {
        ps.setString(1, m.getName());
        ps.setInt(2, m.getCategoryId());
        ps.setString(3, m.getManufacturer());
        ps.setString(4, m.getDosageForm());
        ps.setString(5, m.getStrength());
        ps.setString(6, m.getDescription());
        ps.setBigDecimal(7, m.getPrice());
        ps.setInt(8, m.getStockQuantity());
        ps.setInt(9, m.getReorderLevel());
        ps.setBoolean(10, m.isRequiresPrescription());
        if (m.getExpiryDate() == null) {
            ps.setNull(11, Types.DATE);
        } else {
            ps.setDate(11, Date.valueOf(m.getExpiryDate()));
        }
    }

    /** Turns the current row into a Medicine object. Also used by CartDAOImpl / WishlistDAOImpl. */
    static Medicine mapRow(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setId(rs.getInt("id"));
        m.setName(rs.getString("name"));
        m.setCategoryId(rs.getInt("category_id"));
        m.setCategoryName(rs.getString("category_name"));
        m.setManufacturer(rs.getString("manufacturer"));
        m.setDosageForm(rs.getString("dosage_form"));
        m.setStrength(rs.getString("strength"));
        m.setDescription(rs.getString("description"));
        m.setPrice(rs.getBigDecimal("price"));
        m.setStockQuantity(rs.getInt("stock_quantity"));
        m.setReorderLevel(rs.getInt("reorder_level"));
        m.setRequiresPrescription(rs.getBoolean("requires_prescription"));
        Date expiry = rs.getDate("expiry_date");
        m.setExpiryDate(expiry == null ? null : expiry.toLocalDate());
        m.setDiscontinued(rs.getBoolean("is_discontinued"));
        Timestamp created = rs.getTimestamp("created_at");
        m.setCreatedAt(created == null ? null : created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        m.setUpdatedAt(updated == null ? null : updated.toLocalDateTime());
        return m;
    }

    /** Makes %, _ and [ in the user's search text match literally in LIKE. */
    private String escapeLike(String text) {
        return text.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
    }
}
