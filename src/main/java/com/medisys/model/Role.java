package com.medisys.model;

/**
 * The roles a user can have. AuthFilter uses these to protect pages.
 * The names match the "role" column of the users table.
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
public enum Role {
    CUSTOMER("Customer"),
    PHARMACIST("Senior Pharmacist"),
    ADMIN("Administrator"),
    DELIVERY_STAFF("Delivery Staff");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** Text shown on screen, e.g. "Senior Pharmacist". */
    public String getLabel() {
        return label;
    }

    /** Turns the text from the database into a Role (CUSTOMER if unknown). */
    public static Role fromText(String text) {
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(text)) {
                return role;
            }
        }
        return CUSTOMER;
    }
}
