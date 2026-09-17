package com.medisys.model;

/**
 * The states an order goes through.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 *
 * TODO: add display labels if needed.
 */
public enum OrderStatus {
    PENDING_PAYMENT, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED
}
