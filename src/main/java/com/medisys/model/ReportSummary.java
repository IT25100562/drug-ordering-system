package com.medisys.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The headline numbers of a period (the tiles at the top of the report).
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
public class ReportSummary {

    // Sales (orders that were not cancelled)
    private BigDecimal revenue = BigDecimal.ZERO;
    private int orderCount;
    private BigDecimal cartRevenue = BigDecimal.ZERO;
    private BigDecimal prescriptionRevenue = BigDecimal.ZERO;
    private BigDecimal deliveryFees = BigDecimal.ZERO;
    private int cancelledCount;
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    // Customers
    private int newCustomers;
    private int buyingCustomers;

    // Prescriptions uploaded in the period
    private int prescriptionCount;
    private int approvedCount;
    private int rejectedCount;
    private int correctionCount;
    private int pendingCount;
    private int paidPrescriptionCount;
    private Double averageReviewHours;     // null when nothing was reviewed

    // Deliveries finished in the period
    private int deliveredCount;
    private int onTimeCount;
    private int failedAttempts;

    // ------------------------------------------------------------ helpers

    /** revenue / orders, 0 when there were no orders. */
    public BigDecimal getAverageOrder() {
        return orderCount == 0 ? BigDecimal.ZERO.setScale(2)
                : revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
    }

    /** Approved out of all decided (approved + rejected), in percent, or null. */
    public Double getApprovalRate() {
        int decided = approvedCount + rejectedCount;
        return decided == 0 ? null : approvedCount * 100.0 / decided;
    }

    /** Delivered on or before the expected day, in percent, or null. */
    public Double getOnTimeRate() {
        return deliveredCount == 0 ? null : onTimeCount * 100.0 / deliveredCount;
    }

    /** Share of approved prescriptions that were paid for, in percent, or null. */
    public Double getPrescriptionPayRate() {
        return approvedCount == 0 ? null : paidPrescriptionCount * 100.0 / approvedCount;
    }

    /** Share of revenue that came from prescriptions, in percent, or null. */
    public Double getPrescriptionRevenueShare() {
        return revenue.signum() == 0 ? null
                : prescriptionRevenue.doubleValue() * 100.0 / revenue.doubleValue();
    }

    // ------------------------------------------------- getters and setters

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getCartRevenue() {
        return cartRevenue;
    }

    public void setCartRevenue(BigDecimal cartRevenue) {
        this.cartRevenue = cartRevenue;
    }

    public BigDecimal getPrescriptionRevenue() {
        return prescriptionRevenue;
    }

    public void setPrescriptionRevenue(BigDecimal prescriptionRevenue) {
        this.prescriptionRevenue = prescriptionRevenue;
    }

    public BigDecimal getDeliveryFees() {
        return deliveryFees;
    }

    public void setDeliveryFees(BigDecimal deliveryFees) {
        this.deliveryFees = deliveryFees;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(int cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public int getNewCustomers() {
        return newCustomers;
    }

    public void setNewCustomers(int newCustomers) {
        this.newCustomers = newCustomers;
    }

    public int getBuyingCustomers() {
        return buyingCustomers;
    }

    public void setBuyingCustomers(int buyingCustomers) {
        this.buyingCustomers = buyingCustomers;
    }

    public int getPrescriptionCount() {
        return prescriptionCount;
    }

    public void setPrescriptionCount(int prescriptionCount) {
        this.prescriptionCount = prescriptionCount;
    }

    public int getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(int approvedCount) {
        this.approvedCount = approvedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public int getCorrectionCount() {
        return correctionCount;
    }

    public void setCorrectionCount(int correctionCount) {
        this.correctionCount = correctionCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
    }

    public int getPaidPrescriptionCount() {
        return paidPrescriptionCount;
    }

    public void setPaidPrescriptionCount(int paidPrescriptionCount) {
        this.paidPrescriptionCount = paidPrescriptionCount;
    }

    public Double getAverageReviewHours() {
        return averageReviewHours;
    }

    public void setAverageReviewHours(Double averageReviewHours) {
        this.averageReviewHours = averageReviewHours;
    }

    public int getDeliveredCount() {
        return deliveredCount;
    }

    public void setDeliveredCount(int deliveredCount) {
        this.deliveredCount = deliveredCount;
    }

    public int getOnTimeCount() {
        return onTimeCount;
    }

    public void setOnTimeCount(int onTimeCount) {
        this.onTimeCount = onTimeCount;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }
}
