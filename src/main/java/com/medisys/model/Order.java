package com.medisys.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A placed and paid order, with its lines, payment and status history.
 * One row of the orders table.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
public class Order {

    public static final String SOURCE_CART = "CART";
    public static final String SOURCE_PRESCRIPTION = "PRESCRIPTION";

    private int id;
    private int userId;
    private String customerName;
    private String customerEmail;
    private String source;
    private Integer prescriptionId;      // set for prescription orders
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal total;
    private String deliveryName;
    private String deliveryAddress;
    private String deliveryPhone;
    private String deliveryNote;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<OrderItem> items = new ArrayList<>();
    private Payment payment;
    private List<OrderStatusChange> history = new ArrayList<>();

    // ------------------------------------------------------------ helpers

    /** "ORD-000012", used on screen and in messages. */
    public String getReference() {
        return String.format("ORD-%06d", id);
    }

    public boolean isFromPrescription() {
        return SOURCE_PRESCRIPTION.equals(source);
    }

    /** "RX-000004" for a prescription order, otherwise null. */
    public String getPrescriptionReference() {
        return prescriptionId == null ? null : String.format("RX-%06d", prescriptionId);
    }

    public int getPackCount() {
        int count = 0;
        for (OrderItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    /** e.g. "Panadol 500 mg, Vitamin C 1000 mg". */
    public String getItemSummary() {
        List<String> names = new ArrayList<>();
        for (OrderItem item : items) {
            names.add(item.getMedicineName() + (item.getQuantity() > 1 ? " x" + item.getQuantity() : ""));
        }
        return String.join(", ", names);
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    /** When the order reached a status, or null if it did not (yet). */
    public LocalDateTime getTimeOf(OrderStatus wanted) {
        for (OrderStatusChange change : history) {
            if (change.getStatus() == wanted) {
                return change.getChangedAt();
            }
        }
        return null;
    }

    // ------------------------------------------------- getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Integer prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
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

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
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

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public List<OrderStatusChange> getHistory() {
        return history;
    }

    public void setHistory(List<OrderStatusChange> history) {
        this.history = history;
    }
}
