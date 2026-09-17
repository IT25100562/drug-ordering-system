package com.medisys.model;

import java.math.BigDecimal;

/**
 * One medicine the pharmacist wrote down for an approved prescription:
 * which medicine, how many, how to use it, and the price when it was approved.
 * One row of the prescription_items table.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
public class PrescriptionItem {

    private int id;
    private Medicine medicine;
    private int quantity;
    private String dosageInstructions;
    private BigDecimal unitPrice;       // copied from the medicine at approval time

    public PrescriptionItem() {
    }

    public PrescriptionItem(Medicine medicine, int quantity, String dosageInstructions) {
        this.medicine = medicine;
        this.quantity = quantity;
        this.dosageInstructions = dosageInstructions;
        this.unitPrice = medicine.getPrice();
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = medicine;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
