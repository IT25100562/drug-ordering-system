package com.medisys.model;

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

    /**
     * Why this line cannot be checked out right now (for example "Out of
     * stock"), or null when it is fine. Filled in by CartService.
     */
    private String problem;

    /** Unit price x quantity. */
    public BigDecimal getLineTotal() {
        return medicine.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public boolean hasProblem() {
        return problem != null;
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

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }
}
