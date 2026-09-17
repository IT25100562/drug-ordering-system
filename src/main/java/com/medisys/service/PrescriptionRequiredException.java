package com.medisys.service;

import com.medisys.model.Medicine;

/**
 * Thrown when a customer tries to add a prescription-only medicine to the cart.
 * Those medicines are only ordered through an uploaded prescription, so the
 * servlet sends the customer to the prescription upload page (module 05).
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public class PrescriptionRequiredException extends ValidationException {

    private final Medicine medicine;

    public PrescriptionRequiredException(Medicine medicine) {
        super(medicine.getDisplayName() + " needs a prescription. Upload your prescription and our "
                + "pharmacist will list the medicines for you to pay.");
        this.medicine = medicine;
    }

    public Medicine getMedicine() {
        return medicine;
    }
}
