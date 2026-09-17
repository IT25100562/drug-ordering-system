package com.medisys.dao;

import com.medisys.model.InventorySummary;
import com.medisys.model.Medicine;

import java.sql.SQLException;
import java.util.List;

/**
 * Database operations for medicines.
 *
 * Design pattern: DAO. The rest of the app only knows this interface; the SQL
 * lives in MedicineDAOImpl.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
public interface MedicineDAO {

    // Values for the "filter" parameter of search().
    String FILTER_CATALOG = "CATALOG";           // what customers see: not discontinued, not expired
    String FILTER_ACTIVE = "ACTIVE";             // every medicine that is not discontinued
    String FILTER_LOW_STOCK = "LOW_STOCK";
    String FILTER_OUT_OF_STOCK = "OUT_OF_STOCK";
    String FILTER_EXPIRED = "EXPIRED";
    String FILTER_DISCONTINUED = "DISCONTINUED";

    /**
     * Finds medicines.
     *
     * @param keyword    part of the name or manufacturer, or null for all
     * @param categoryId only this category, or null for all
     * @param filter     one of the FILTER_ constants
     */
    List<Medicine> search(String keyword, Integer categoryId, String filter) throws SQLException;

    /** Returns the medicine, or null when the id does not exist. */
    Medicine findById(int id) throws SQLException;

    /** True if another medicine (not excludeId) has the same name and strength. */
    boolean existsByNameAndStrength(String name, String strength, int excludeId) throws SQLException;

    /** Inserts the medicine and returns its new id. */
    int create(Medicine medicine) throws SQLException;

    boolean update(Medicine medicine) throws SQLException;

    /** Adds quantity to the stock. */
    boolean addStock(int id, int quantity) throws SQLException;

    /**
     * Takes quantity out of the stock, only if there is enough.
     * Returns false when the stock is too low (nothing is changed).
     */
    boolean reduceStock(int id, int quantity) throws SQLException;

    boolean setDiscontinued(int id, boolean discontinued) throws SQLException;

    InventorySummary getSummary() throws SQLException;
}
