package com.medisys.dao;

import com.medisys.model.CartItem;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Database operations for the shopping cart.
 *
 * A customer has at most one line per medicine, so a line is found by
 * (userId, medicineId).
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public interface CartDAO {

    /** All lines of the customer's cart, newest first, with their medicine. */
    List<CartItem> findByUser(int userId) throws SQLException;

    /** The quantity of this medicine in the cart, or 0 when it is not there. */
    int findQuantity(int userId, int medicineId) throws SQLException;

    /** medicineId -> quantity, for showing "2 in cart" on the catalog. */
    Map<Integer, Integer> findQuantities(int userId) throws SQLException;

    /** Adds a new line. */
    void add(int userId, int medicineId, int quantity) throws SQLException;

    /** Changes the quantity of an existing line. */
    boolean updateQuantity(int userId, int medicineId, int quantity) throws SQLException;

    boolean remove(int userId, int medicineId) throws SQLException;

    /** Empties the cart (module 02 calls this after an order is placed). */
    void clear(int userId) throws SQLException;

    /** Total number of packs in the cart (sum of quantities). */
    int countItems(int userId) throws SQLException;
}
