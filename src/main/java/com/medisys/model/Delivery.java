package com.medisys.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Delivery record for a paid order: the rider, the status and its history.
 * One row of the deliveries table, plus a few order and customer details
 * that the delivery pages show (read with a JOIN, not stored twice).
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
public class Delivery {

    private int id;
    private int orderId;
    private Integer staffId;          // null until a rider is assigned
    private DeliveryStatus status;
    private int attempts;
    private LocalDate estimatedDate;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // From the joined tables
    private String staffName;
    private String staffPhone;
    private int customerId;
    private String customerName;
    private OrderStatus orderStatus;
    private String deliveryName;
    private String deliveryAddress;
    private String deliveryPhone;
    private String deliveryNote;

    private List<DeliveryUpdate> updates = new ArrayList<>();

    // ------------------------------------------------------------ helpers

    /** "DEL-000012", used on screen and in messages. */
    public String getReference() {
        return String.format("DEL-%06d", id);
    }

    /** The order number, "ORD-000012". */
    public String getOrderReference() {
        return String.format("ORD-%06d", orderId);
    }

    public boolean hasRider() {
        return staffId != null;
    }

    /** True while the parcel is late: not delivered and the expected day has passed. */
    public boolean isLate() {
        return !status.isFinished() && estimatedDate != null && estimatedDate.isBefore(LocalDate.now());
    }

    /** When the delivery first reached a status, or null if it did not (yet). */
    public LocalDateTime getTimeOf(DeliveryStatus wanted) {
        for (DeliveryUpdate update : updates) {
            if (update.getStatus() == wanted) {
                return update.getCreatedAt();
            }
        }
        return null;
    }

    /** The newest update, or null. */
    public DeliveryUpdate getLastUpdate() {
        return updates.isEmpty() ? null : updates.get(updates.size() - 1);
    }

    // ------------------------------------------------- getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public LocalDate getEstimatedDate() {
        return estimatedDate;
    }

    public void setEstimatedDate(LocalDate estimatedDate) {
        this.estimatedDate = estimatedDate;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffPhone() {
        return staffPhone;
    }

    public void setStaffPhone(String staffPhone) {
        this.staffPhone = staffPhone;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getDeliveryName() {
        return deliveryName;
    }

    public void setDeliveryName(String deliveryName) {
        this.deliveryName = deliveryName;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getDeliveryPhone() {
        return deliveryPhone;
    }

    public void setDeliveryPhone(String deliveryPhone) {
        this.deliveryPhone = deliveryPhone;
    }

    public String getDeliveryNote() {
        return deliveryNote;
    }

    public void setDeliveryNote(String deliveryNote) {
        this.deliveryNote = deliveryNote;
    }

    public List<DeliveryUpdate> getUpdates() {
        return updates;
    }

    public void setUpdates(List<DeliveryUpdate> updates) {
        this.updates = updates;
    }
}
