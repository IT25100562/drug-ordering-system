package com.medisys.model;

import java.time.LocalDateTime;

/**
 * One step of a delivery ("Out for delivery" at 10:32 by Ruwan).
 * One row of the delivery_updates table.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
public class DeliveryUpdate {

    private DeliveryStatus status;
    private String note;
    private String updatedByName;     // null = the system
    private LocalDateTime createdAt;

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
