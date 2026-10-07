package com.medisys.model;

import java.time.LocalDateTime;

/**
 * One step in an order's history ("Being packed" at 10:32 by Admin).
 * One row of the order_status_history table.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
public class OrderStatusChange {

    private OrderStatus status;
    private String note;
    private String changedByName;     // null = the customer or the system
    private LocalDateTime changedAt;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public void setChangedByName(String changedByName) {
        this.changedByName = changedByName;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
