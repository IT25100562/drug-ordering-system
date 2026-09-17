package com.medisys.model;

/**
 * The states an order goes through. Every order is paid when it is placed.
 *
 *   PAID -> PROCESSING -> SHIPPED -> DELIVERED
 *   PAID or PROCESSING -> CANCELLED
 *
 * The names match the "status" column of the orders table.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
public enum OrderStatus {
    PAID("Order placed", "badge-pending"),
    PROCESSING("Being packed", "badge-correction"),
    SHIPPED("Out for delivery", "badge-paid"),
    DELIVERED("Delivered", "badge-approved"),
    CANCELLED("Cancelled", "badge-expired");

    private final String label;
    private final String cssClass;

    OrderStatus(String label, String cssClass) {
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

    /** The next step in the normal flow, or null when there is none. */
    public OrderStatus next() {
        switch (this) {
            case PAID:
                return PROCESSING;
            case PROCESSING:
                return SHIPPED;
            case SHIPPED:
                return DELIVERED;
            default:
                return null;
        }
    }

    /** The pharmacy can still cancel before the parcel leaves. */
    public boolean canBeCancelledByPharmacy() {
        return this == PAID || this == PROCESSING;
    }

    /** The customer can cancel only before packing starts. */
    public boolean canBeCancelledByCustomer() {
        return this == PAID;
    }

    /** Turns the text from the database or a URL into a status, or null if it is not one. */
    public static OrderStatus fromText(String text) {
        for (OrderStatus status : values()) {
            if (status.name().equalsIgnoreCase(text)) {
                return status;
            }
        }
        return null;
    }
}
