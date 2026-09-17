package com.medisys.model;

/**
 * A medicine category (Pain Relief, Antibiotics, ...). One row of the
 * categories table.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
public class Category {

    private int id;
    private String name;
    private String description;
    private int medicineCount;   // how many medicines use it (filled by the DAO)

    public Category() {
    }

    public Category(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMedicineCount() {
        return medicineCount;
    }

    public void setMedicineCount(int medicineCount) {
        this.medicineCount = medicineCount;
    }
}
