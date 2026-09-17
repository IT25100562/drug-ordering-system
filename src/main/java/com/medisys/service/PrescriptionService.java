package com.medisys.service;

import java.sql.SQLException;

/**
 * All verification rules for prescriptions, kept in one class.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 *
 * TODO: JPG/PNG/PDF only, max 5 MB, safe stored file name, note required for
 *       reject/correction, decide only while PENDING, no delete when paid.
 */
public class PrescriptionService {

    /**
     * Used by module 01 (cart): may this customer buy this prescription-only
     * medicine? True only when a pharmacist has APPROVED a prescription for it
     * that has not been used for an order yet.
     *
     * TODO (module 05): look it up in the prescriptions table. Until then no
     *       prescription is ever approved, so prescription-only medicines
     *       cannot be added to the cart.
     */
    public boolean hasApprovedPrescription(int userId, int medicineId) throws SQLException {
        return false;
    }
}
