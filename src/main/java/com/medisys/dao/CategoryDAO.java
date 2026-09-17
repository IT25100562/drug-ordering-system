package com.medisys.dao;

import com.medisys.model.Category;

import java.sql.SQLException;
import java.util.List;

/**
 * Database operations for medicine categories.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
public interface CategoryDAO {

    /** All categories, A to Z, each with its medicine count. */
    List<Category> findAll() throws SQLException;

    /** Returns the category, or null when the id does not exist. */
    Category findById(int id) throws SQLException;

    boolean existsByName(String name) throws SQLException;

    int create(Category category) throws SQLException;

    boolean delete(int id) throws SQLException;

    /** How many medicines (including discontinued ones) use this category. */
    int countMedicines(int id) throws SQLException;
}
