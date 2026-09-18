package com.medisys.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A prescription a customer uploaded (a photo or PDF). One row of the
 * prescriptions table, together with the customer, the medicines the
 * pharmacist wrote down, and the payment (from the order that paid for it).
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
public class Prescription {

    /** A prescription uploaded more than this many days ago, and not paid, is expired. */
    public static final int EXPIRY_DAYS = 30;

    private int id;

    // The customer who uploaded it (from the users table).
    private int userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;
    private boolean customerFlagged;     // red flag from a pharmacist (user accounts)
    private boolean customerHasPhoto;

    private String customerNote;

    // The stored file.
    private String fileKey;
    private String originalFileName;
    private String contentType;
    private int fileSize;

    // The pharmacist's decision.
    private PrescriptionStatus status;
    private String pharmacistNote;
    private Integer reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private List<PrescriptionItem> items = new ArrayList<>();

    private int correctionCount;     // how many corrected copies were uploaded
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;

    // The payment: read from the order that paid for it (module 02).
    private Integer orderId;
    private String orderStatus;
    private LocalDateTime paidAt;
    private BigDecimal amountPaid;
    private String paymentReference;
    private String cardLast4;
    private String deliveryName;
    private String deliveryAddress;
    private String deliveryPhone;

    // ------------------------------------------------------------ helpers

    public boolean isPaid() {
        return orderId != null;
    }

    /** "ORD-000012", or null when not paid. */
    public String getOrderReference() {
        return orderId == null ? null : String.format("ORD-%06d", orderId);
    }

    /** Uploaded more than EXPIRY_DAYS ago and not paid. */
    public boolean isExpired() {
        return !isPaid() && uploadedAt != null
                && uploadedAt.plusDays(EXPIRY_DAYS).isBefore(LocalDateTime.now());
    }

    /** The last day it can be paid for. */
    public LocalDateTime getExpiresAt() {
        return uploadedAt == null ? null : uploadedAt.plusDays(EXPIRY_DAYS);
    }

    /** The pharmacist can only decide while it is waiting. */
    public boolean isAwaitingReview() {
        return status == PrescriptionStatus.PENDING;
    }

    /** Approved, not paid yet and not expired: the customer can pay. */
    public boolean isPayable() {
        return status == PrescriptionStatus.APPROVED && !isPaid() && !isExpired();
    }

    public boolean needsCorrection() {
        return status == PrescriptionStatus.CORRECTION_REQUESTED;
    }

    /** A paid prescription is the record of a sale, so it is kept. */
    public boolean isDeletable() {
        return !isPaid();
    }

    /** Sum of all medicine lines. */
    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (PrescriptionItem item : items) {
            total = total.add(item.getLineTotal());
        }
        return total;
    }

    /** Total number of packs. */
    public int getPackCount() {
        int count = 0;
        for (PrescriptionItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    /** e.g. "Metformin 500 mg, Panadol 500 mg". */
    public String getMedicineNames() {
        List<String> names = new ArrayList<>();
        for (PrescriptionItem item : items) {
            names.add(item.getMedicine().getDisplayName());
        }
        return String.join(", ", names);
    }

    public boolean isPdf() {
        return "application/pdf".equals(contentType);
    }

    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    /** e.g. "245 KB" or "1.3 MB". */
    public String getFileSizeLabel() {
        if (fileSize < 1024 * 1024) {
            return Math.max(1, Math.round(fileSize / 1024.0)) + " KB";
        }
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    /** "RX-000012", used on screen and in messages. */
    public String getReference() {
        return String.format("RX-%06d", id);
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

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public boolean isCustomerFlagged() {
        return customerFlagged;
    }

    public void setCustomerFlagged(boolean customerFlagged) {
        this.customerFlagged = customerFlagged;
    }

    public boolean isCustomerHasPhoto() {
        return customerHasPhoto;
    }

    public void setCustomerHasPhoto(boolean customerHasPhoto) {
        this.customerHasPhoto = customerHasPhoto;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    public void setCustomerNote(String customerNote) {
        this.customerNote = customerNote;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public PrescriptionStatus getStatus() {
        return status;
    }

    public void setStatus(PrescriptionStatus status) {
        this.status = status;
    }

    public String getPharmacistNote() {
        return pharmacistNote;
    }

    public void setPharmacistNote(String pharmacistNote) {
        this.pharmacistNote = pharmacistNote;
    }

    public Integer getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(Integer reviewedById) {
        this.reviewedById = reviewedById;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public List<PrescriptionItem> getItems() {
        return items;
    }

    public void setItems(List<PrescriptionItem> items) {
        this.items = items;
    }

    public int getCorrectionCount() {
        return correctionCount;
    }

    public void setCorrectionCount(int correctionCount) {
        this.correctionCount = correctionCount;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    /** The status name of the paying order, e.g. "SHIPPED" (null when not paid). */
    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
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
}
