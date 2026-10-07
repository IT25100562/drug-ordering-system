package com.medisys.service;

import com.medisys.dao.CartDAO;
import com.medisys.dao.impl.CartDAOImpl;
import com.medisys.model.Cart;
import com.medisys.model.CartItem;
import com.medisys.model.Medicine;
import com.medisys.util.TextUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * All business rules of the shopping cart.
 *
 *  - only medicines that are on sale (not discontinued / expired / out of stock)
 *    can be added
 *  - a prescription-only medicine never goes into the cart: the customer
 *    uploads the prescription (module 05), the pharmacist lists the medicines,
 *    and the customer pays for that prescription directly
 *  - the quantity of one medicine is 1 .. min(stock, MAX_QUANTITY_PER_ITEM)
 *  - adding a medicine that is already in the cart increases its quantity
 *  - the cart is checked again every time it is shown, because prices and
 *    stock may have changed since the item was added
 *
 * Module 02 (checkout) uses getCart(), and clearCart() after the order.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public class CartService {

    /** A pharmacy sells at most this many packs of one medicine per order. */
    public static final int MAX_QUANTITY_PER_ITEM = 10;

    // SQL Server error numbers for "duplicate key".
    private static final int DUPLICATE_KEY = 2627;
    private static final int DUPLICATE_INDEX = 2601;

    private final CartDAO cartDAO = new CartDAOImpl();
    private final MedicineService medicineService = new MedicineService();

    /** The customer's cart, with a problem message on every line that cannot be bought. */
    public Cart getCart(int userId) throws SQLException {
        List<CartItem> items = cartDAO.findByUser(userId);
        for (CartItem item : items) {
            item.setMaxQuantity(maxQuantity(userId, item.getMedicine()));
            item.setProblem(findProblem(userId, item));
        }
        return new Cart(items);
    }

    /** Number of packs in the cart, for the badge in the menu. */
    public int countItems(int userId) throws SQLException {
        return cartDAO.countItems(userId);
    }

    /** medicineId -> quantity in the cart. */
    public Map<Integer, Integer> getQuantities(int userId) throws SQLException {
        return cartDAO.findQuantities(userId);
    }

    public int getQuantity(int userId, int medicineId) throws SQLException {
        return cartDAO.findQuantity(userId, medicineId);
    }

    /** The most of this medicine anyone can have in the cart (stock and the per-order limit). */
    public int maxQuantity(Medicine medicine) {
        return Math.max(0, Math.min(medicine.getStockQuantity(), MAX_QUANTITY_PER_ITEM));
    }

    /**
     * The most of this medicine this customer can have in the cart
     * (0 for a prescription-only medicine).
     */
    public int maxQuantity(int userId, Medicine medicine) {
        return medicine.isRequiresPrescription() ? 0 : maxQuantity(medicine);
    }

    /** True when the medicine can only be ordered by uploading a prescription. */
    public boolean needsPrescriptionUpload(int userId, Medicine medicine) {
        return medicine.isRequiresPrescription();
    }

    /**
     * Adds a medicine to the cart (or raises its quantity).
     *
     * @return a message for the customer, e.g. "Panadol 500 mg added to your cart."
     * @throws PrescriptionRequiredException when a prescription must be uploaded first
     * @throws ValidationException           for any other rule that is broken
     */
    public String addToCart(int userId, int medicineId, String quantityText)
            throws SQLException, ValidationException {
        Integer quantity = TextUtil.parseInt(quantityText);
        if (quantity == null || quantity < 1) {
            throw new ValidationException("Please choose a quantity of at least 1.");
        }

        // Throws "not available" / "out of stock".
        Medicine medicine = medicineService.getAvailableMedicine(medicineId);

        if (needsPrescriptionUpload(userId, medicine)) {
            throw new PrescriptionRequiredException(medicine);
        }

        int limit = maxQuantity(userId, medicine);
        int inCart = cartDAO.findQuantity(userId, medicineId);
        if (inCart >= limit) {
            throw new ValidationException("You already have the most you can buy of "
                    + medicine.getDisplayName() + " (" + limit + ") in your cart.");
        }

        int newQuantity = Math.min(inCart + quantity, limit);
        if (inCart == 0) {
            try {
                cartDAO.add(userId, medicineId, newQuantity);
            } catch (SQLException e) {
                // Two clicks at the same moment: the line was just created, so update it.
                if (e.getErrorCode() != DUPLICATE_KEY && e.getErrorCode() != DUPLICATE_INDEX) {
                    throw e;
                }
                cartDAO.updateQuantity(userId, medicineId, newQuantity);
            }
        } else {
            cartDAO.updateQuantity(userId, medicineId, newQuantity);
        }

        String name = medicine.getDisplayName();
        if (newQuantity < inCart + quantity) {
            return "Only " + limit + " of " + name + " can be bought, so your cart now has " + limit + ".";
        }
        if (inCart > 0) {
            return "You now have " + newQuantity + " of " + name + " in your cart.";
        }
        return name + " was added to your cart.";
    }

    /**
     * Sets the quantity of a line. 0 removes the line.
     *
     * @return a message for the customer
     */
    public String updateQuantity(int userId, int medicineId, String quantityText)
            throws SQLException, ValidationException {
        Integer quantity = TextUtil.parseInt(quantityText);
        if (quantity == null || quantity < 0) {
            throw new ValidationException("Quantity must be a whole number.");
        }
        Medicine medicine = requireLine(userId, medicineId);

        if (quantity == 0) {
            cartDAO.remove(userId, medicineId);
            return medicine.getDisplayName() + " was removed from your cart.";
        }
        if (quantity > MAX_QUANTITY_PER_ITEM) {
            throw new ValidationException("You can buy at most " + MAX_QUANTITY_PER_ITEM + " of "
                    + medicine.getDisplayName() + ".");
        }
        if (quantity > medicine.getStockQuantity()) {
            throw new ValidationException(medicine.getStockQuantity() == 0
                    ? medicine.getDisplayName() + " is out of stock."
                    : "Only " + medicine.getStockQuantity() + " of " + medicine.getDisplayName() + " left in stock.");
        }
        if (medicine.isRequiresPrescription()) {
            throw new ValidationException(medicine.getDisplayName() + " can only be ordered with a prescription.");
        }
        cartDAO.updateQuantity(userId, medicineId, quantity);
        return "Quantity of " + medicine.getDisplayName() + " changed to " + quantity + ".";
    }

    /** Removes a line and returns the medicine that was removed. */
    public Medicine removeItem(int userId, int medicineId) throws SQLException, ValidationException {
        Medicine medicine = requireLine(userId, medicineId);
        cartDAO.remove(userId, medicineId);
        return medicine;
    }

    /** Empties the cart. Module 02 calls this after the order is placed. */
    public void clearCart(int userId) throws SQLException {
        cartDAO.clear(userId);
    }

    // ============================================================ helpers

    /** Why a line cannot be checked out, or null when it can. */
    private String findProblem(int userId, CartItem item) throws SQLException {
        Medicine m = item.getMedicine();
        if (m.isDiscontinued() || m.isExpired()) {
            return "No longer available. Please remove it from your cart.";
        }
        if (m.isOutOfStock()) {
            return "Out of stock right now. Remove it or save it for later.";
        }
        if (item.getQuantity() > m.getStockQuantity()) {
            return "Only " + m.getStockQuantity() + " left in stock. Please lower the quantity.";
        }
        if (m.isRequiresPrescription()) {
            return "Prescription-only. Remove it and upload your prescription instead.";
        }
        return null;
    }

    /** The medicine of a cart line, or an error when the line is not in this customer's cart. */
    private Medicine requireLine(int userId, int medicineId) throws SQLException, ValidationException {
        if (cartDAO.findQuantity(userId, medicineId) == 0) {
            throw new ValidationException("That item is no longer in your cart.");
        }
        Medicine medicine = medicineService.getMedicine(medicineId);
        if (medicine == null) {
            throw new ValidationException("That item is no longer in your cart.");
        }
        return medicine;
    }
}
