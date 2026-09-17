package com.medisys.service;

import com.medisys.model.Medicine;

/**
 * Thrown when a customer tries to add a prescription-only medicine to the cart
 * without an approved prescription. The servlet then sends the customer to the
 * prescription upload page (module 05) for that medicine.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public class PrescriptionRequiredException extends ValidationException {

    private final Medicine medicine;

    public PrescriptionRequiredException(Medicine medicine) {
        super(medicine.getDisplayName() + " needs a prescription. Upload it and our pharmacist "
                + "will check it before you can add it to your cart.");
        this.medicine = medicine;
    }

    public Medicine getMedicine() {
        return medicine;
    }
}
