package com.medisys.order;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The (test) card payment of an order. Card numbers are never stored, only the
 * last 4 digits. One row of the payments table.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
public class Payment {

    public static final String PAID = "PAID";
    public static final String REFUNDED = "REFUNDED";

    private static final SecureRandom RANDOM = new SecureRandom();

    /** A new reference for the receipt, e.g. PAY-20260917-4F7K2Q. */
    public static String newReference() {
        String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(letters.charAt(RANDOM.nextInt(letters.length())));
        }
        return "PAY-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + code;
    }

    private int id;
    private int orderId;
    private BigDecimal amount;
    private String method;
    private String cardLast4;
    private String reference;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    public boolean isRefunded() {
        return REFUNDED.equals(status);
    }

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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
}
