package com.medisys.model;

import java.time.LocalDateTime;

/**
 * One medicine saved to a customer's wishlist. One row of wishlist_items,
 * together with the medicine it points to.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public class WishlistItem {

    private int id;
    private int userId;
    private Medicine medicine;
    private LocalDateTime addedAt;

    /** True when it can be moved to the cart straight away. */
    public boolean isAvailable() {
        return medicine.isAvailable();
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

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
