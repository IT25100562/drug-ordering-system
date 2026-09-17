package com.medisys.service;

import com.medisys.dao.WishlistDAO;
import com.medisys.dao.impl.WishlistDAOImpl;
import com.medisys.model.Medicine;
import com.medisys.model.WishlistItem;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * Business rules for the wishlist.
 *
 *  - any medicine in the catalog can be saved, even when it is out of stock
 *    (that is the point: "tell me later")
 *  - a medicine is saved only once
 *  - "Move to cart" follows all the cart rules, and only removes the item from
 *    the wishlist when it really went into the cart
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public class WishlistService {

    private final WishlistDAO wishlistDAO = new WishlistDAOImpl();
    private final MedicineService medicineService = new MedicineService();
    private final CartService cartService = new CartService();

    public List<WishlistItem> getWishlist(int userId) throws SQLException {
        return wishlistDAO.findByUser(userId);
    }

    /** Ids of the saved medicines. */
    public Set<Integer> getMedicineIds(int userId) throws SQLException {
        return wishlistDAO.findMedicineIds(userId);
    }

    public boolean isSaved(int userId, int medicineId) throws SQLException {
        return wishlistDAO.exists(userId, medicineId);
    }

    public int count(int userId) throws SQLException {
        return wishlistDAO.count(userId);
    }

    /** Saves a medicine. Returns the medicine. */
    public Medicine add(int userId, int medicineId) throws SQLException, ValidationException {
        Medicine medicine = medicineService.getCatalogMedicine(medicineId);
        if (medicine == null) {
            throw new ValidationException("This medicine is not available.");
        }
        wishlistDAO.add(userId, medicineId);
        return medicine;
    }

    /** Removes a saved medicine. Returns the medicine (may be null if it was deleted). */
    public Medicine remove(int userId, int medicineId) throws SQLException, ValidationException {
        if (!wishlistDAO.remove(userId, medicineId)) {
            throw new ValidationException("That item is not in your wishlist.");
        }
        return medicineService.getMedicine(medicineId);
    }

    /**
     * Saves the medicine if it is not saved, removes it if it is.
     *
     * @return true when the medicine is saved after the call
     */
    public boolean toggle(int userId, int medicineId) throws SQLException, ValidationException {
        if (wishlistDAO.exists(userId, medicineId)) {
            wishlistDAO.remove(userId, medicineId);
            return false;
        }
        add(userId, medicineId);
        return true;
    }

    /** Puts one pack in the cart, then removes it from the wishlist. */
    public String moveToCart(int userId, int medicineId) throws SQLException, ValidationException {
        String message = cartService.addToCart(userId, medicineId, "1");   // throws if not allowed
        wishlistDAO.remove(userId, medicineId);
        return message;
    }

    /** "Save for later" on the cart page: into the wishlist, out of the cart. */
    public Medicine saveForLater(int userId, int medicineId) throws SQLException, ValidationException {
        Medicine medicine = cartService.removeItem(userId, medicineId);
        wishlistDAO.add(userId, medicineId);
        return medicine;
    }
}
