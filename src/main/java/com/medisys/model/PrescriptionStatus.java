package com.medisys.model;

/**
 * The states of a prescription, from the activity diagram.
 *
 *   PENDING               uploaded, waiting for the pharmacist ("Verification Pending")
 *   APPROVED              the pharmacist accepted it ("Approved for Checkout")
 *   REJECTED              the pharmacist refused it (a note says why)
 *   CORRECTION_REQUESTED  the copy was unclear; the customer must upload a new one
 *
 * The names match the "status" column of the prescriptions table.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
public enum PrescriptionStatus {
    PENDING("Verification Pending", "badge-pending"),
    APPROVED("Approved for Checkout", "badge-approved"),
    REJECTED("Rejected", "badge-rejected"),
    CORRECTION_REQUESTED("Correction Requested", "badge-correction");

    private final String label;
    private final String cssClass;

    PrescriptionStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String getLabel() {
        return label;
    }

    /** CSS class for the coloured status badge (see style.css). */
    public String getCssClass() {
        return cssClass;
    }

    /** Turns the text from the database or a URL into a status, or null if it is not one. */
    public static PrescriptionStatus fromText(String text) {
        for (PrescriptionStatus status : values()) {
            if (status.name().equalsIgnoreCase(text)) {
                return status;
            }
        }
        return null;
    }
}
