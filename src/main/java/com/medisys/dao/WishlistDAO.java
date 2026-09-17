package com.medisys.dao;

import com.medisys.model.WishlistItem;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Database operations for the wishlist.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public interface WishlistDAO {

    /** The customer's saved medicines, newest first. */
    List<WishlistItem> findByUser(int userId) throws SQLException;

    /** Ids of the saved medicines, for the filled hearts on the catalog. */
    Set<Integer> findMedicineIds(int userId) throws SQLException;

    boolean exists(int userId, int medicineId) throws SQLException;

    void add(int userId, int medicineId) throws SQLException;

    boolean remove(int userId, int medicineId) throws SQLException;

    int count(int userId) throws SQLException;
}
