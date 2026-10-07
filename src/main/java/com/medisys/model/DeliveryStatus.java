package com.medisys.model;

import java.util.List;

/**
 * The states a delivery goes through.
 *
 *   PENDING -> DISPATCHED -> OUT_FOR_DELIVERY -> DELIVERED
 *                            OUT_FOR_DELIVERY -> FAILED -> OUT_FOR_DELIVERY (try again)
 *   PENDING -> CANCELLED    (only when the order itself is cancelled)
 *
 * The names match the "status" column of the deliveries table.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
public enum DeliveryStatus {
    PENDING("Preparing", "badge-pending"),
    DISPATCHED("Dispatched", "badge-correction"),
    OUT_FOR_DELIVERY("Out for delivery", "badge-paid"),
    DELIVERED("Delivered", "badge-approved"),
    FAILED("Delivery attempt failed", "badge-rejected"),
    CANCELLED("Cancelled", "badge-expired");

    private final String label;
    private final String cssClass;

    DeliveryStatus(String label, String cssClass) {
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

    /** The statuses a rider may move a delivery to from this one. */
    public List<DeliveryStatus> nextSteps() {
        switch (this) {
            case PENDING:
                return List.of(DISPATCHED);
            case DISPATCHED:
                return List.of(OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY:
                return List.of(DELIVERED, FAILED);
            case FAILED:
                return List.of(OUT_FOR_DELIVERY);
            default:
                return List.of();
        }
    }

    /** DELIVERED and CANCELLED are final: nothing can change any more. */
    public boolean isFinished() {
        return this == DELIVERED || this == CANCELLED;
    }

    /** The rider can still be changed while the parcel is at the pharmacy (or came back). */
    public boolean canAssignRider() {
        return this == PENDING || this == FAILED;
    }

    /** The button text for moving a delivery to this status. */
    public String getActionLabel() {
        switch (this) {
            case DISPATCHED:
                return "Picked up from the pharmacy";
            case OUT_FOR_DELIVERY:
                return "On the way to the customer";
            case DELIVERED:
                return "Delivered";
            case FAILED:
                return "Could not deliver";
            default:
                return label;
        }
    }

    /** Turns the text from the database or a form into a status, or null if it is not one. */
    public static DeliveryStatus fromText(String text) {
        for (DeliveryStatus status : values()) {
            if (status.name().equalsIgnoreCase(text)) {
                return status;
            }
        }
        return null;
    }
}
