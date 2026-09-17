package com.medisys.dao;

/**
 * Thrown while placing an order when a medicine does not have enough stock.
 * Nothing was saved (the transaction was rolled back).
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
public class StockShortageException extends Exception {

    private final int medicineId;

    public StockShortageException(int medicineId) {
        super("Not enough stock for medicine " + medicineId);
        this.medicineId = medicineId;
    }

    public int getMedicineId() {
        return medicineId;
    }
}
