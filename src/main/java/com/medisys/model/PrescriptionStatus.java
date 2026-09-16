package com.medisys.model;

/**
 * The states of a prescription, from the activity diagram.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 *
 * TODO: add display labels (PENDING = Verification Pending, APPROVED = Approved for Checkout).
 */
public enum PrescriptionStatus {
    PENDING, APPROVED, REJECTED, CORRECTION_REQUESTED
}
