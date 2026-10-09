package com.medisys.cart;

import com.medisys.medicine.Medicine;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One medicine line in a customer's shopping cart. One row of cart_items,
 * together with the medicine it points to.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public class CartItem {

    private int id;
    private int userId;
    private Medicine medicine;
    private int quantity;
    private LocalDateTime addedAt;

    /** Unit price x quantity. */
    public BigDecimal getLineTotal() {
        return medicine.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Why this line cannot be checked out right now, or null when it is fine.
     * Checked every time the cart is shown, because the price, stock or
     * medicine may have changed since it was added.
     */
    public String getProblem() {
        if (medicine.isDiscontinued() || medicine.isExpired()) {
            return "No longer available. Please remove it from your cart.";
        }
        if (medicine.isOutOfStock()) {
            return "Out of stock right now. Remove it or save it for later.";
        }
        if (quantity > medicine.getStockQuantity()) {
            return "Only " + medicine.getStockQuantity() + " left in stock. Please lower the quantity.";
        }
        if (medicine.isRequiresPrescription()) {
            return "Prescription-only. Remove it and upload your prescription instead.";
        }
        return null;
    }

    public boolean hasProblem() {
        return getProblem() != null;
    }

    /** The most the customer may buy of this medicine right now (0 for a prescription-only one). */
    public int getMaxQuantity() {
        return medicine.isRequiresPrescription() ? 0 : Cart.maxQuantity(medicine);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
