package com.medisys.model;

import java.math.BigDecimal;

/**
 * One medicine in an order. The name and price are copied when the order is
 * placed. One row of the order_items table.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
public class OrderItem {

    private int id;
    private int medicineId;
    private String medicineName;         // e.g. "Panadol 500 mg"
    private String dosageForm;
    private BigDecimal unitPrice;
    private int quantity;
    private String dosageInstructions;   // only for prescription orders

    public OrderItem() {
    }

    /** A new line for a medicine, with its current name and price. */
    public OrderItem(Medicine medicine, BigDecimal unitPrice, int quantity, String dosageInstructions) {
        this.medicineId = medicine.getId();
        this.medicineName = medicine.getDisplayName();
        this.dosageForm = medicine.getDosageForm();
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.dosageInstructions = dosageInstructions;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /** Short text for the coloured tile, e.g. "TAB" (same as Medicine.getThumbText). */
    public String getThumbText() {
        return dosageForm == null || dosageForm.isEmpty() ? "MED"
                : dosageForm.substring(0, Math.min(3, dosageForm.length())).toUpperCase();
    }

    public String getThumbCssClass() {
        return "thumb thumb-" + (dosageForm == null ? "other" : dosageForm.toLowerCase());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDosageInstructions() {
        return dosageInstructions;
    }

    public void setDosageInstructions(String dosageInstructions) {
        this.dosageInstructions = dosageInstructions;
    }
}
