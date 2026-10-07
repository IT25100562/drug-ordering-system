package com.medisys.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A medicine sold in the shop. One row of the medicines table.
 *
 * Other modules use this class too:
 *   01 (cart)         - price, isAvailable(), requiresPrescription
 *   02 (orders)       - price, stock
 *   05 (prescription) - requiresPrescription
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
public class Medicine {

    /** The dosage forms a medicine can have (shown as a drop-down list). */
    public static final String[] DOSAGE_FORMS = {
            "Tablet", "Capsule", "Syrup", "Suspension", "Injection",
            "Cream", "Ointment", "Drops", "Inhaler", "Liquid", "Powder", "Other"
    };

    private int id;
    private String name;
    private int categoryId;
    private String categoryName;      // filled from the categories table (JOIN)
    private String manufacturer;
    private String dosageForm;
    private String strength;          // e.g. "500 mg"
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private int reorderLevel;         // low stock when stock <= this
    private boolean requiresPrescription;
    private LocalDate expiryDate;     // may be null
    private boolean discontinued;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ------------------------------------------------------------ helpers

    public boolean isOutOfStock() {
        return stockQuantity <= 0;
    }

    /** Some stock left, but at or below the reorder level. */
    public boolean isLowStock() {
        return stockQuantity > 0 && stockQuantity <= reorderLevel;
    }

    /** True when the expiry date has passed. */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /** True when a customer can buy it right now. */
    public boolean isAvailable() {
        return !discontinued && !isExpired() && !isOutOfStock();
    }

    /** Short text for the stock badge. */
    public String getStockLabel() {
        if (isOutOfStock()) {
            return "Out of stock";
        }
        if (isLowStock()) {
            return "Low stock";
        }
        return "In stock";
    }

    /** CSS class for the stock badge (see style.css). */
    public String getStockCssClass() {
        if (isOutOfStock()) {
            return "badge-rejected";
        }
        if (isLowStock()) {
            return "badge-pending";
        }
        return "badge-approved";
    }

    /**
     * CSS class for the small coloured tile shown instead of a photo,
     * e.g. "thumb thumb-syrup".
     */
    public String getThumbCssClass() {
        String form = dosageForm == null ? "other" : dosageForm.toLowerCase();
        return "thumb thumb-" + form;
    }

    /** Short text inside that tile, e.g. "TAB" or "SYR". */
    public String getThumbText() {
        if (dosageForm == null || dosageForm.isEmpty()) {
            return "MED";
        }
        return dosageForm.substring(0, Math.min(3, dosageForm.length())).toUpperCase();
    }

    /** Name with strength, e.g. "Panadol 500 mg". */
    public String getDisplayName() {
        return strength == null || strength.isBlank() ? name : name + " " + strength;
    }

    // ------------------------------------------------- getters and setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public boolean isRequiresPrescription() {
        return requiresPrescription;
    }

    public void setRequiresPrescription(boolean requiresPrescription) {
        this.requiresPrescription = requiresPrescription;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isDiscontinued() {
        return discontinued;
    }

    public void setDiscontinued(boolean discontinued) {
        this.discontinued = discontinued;
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
}
