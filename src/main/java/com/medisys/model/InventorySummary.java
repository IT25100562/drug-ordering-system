package com.medisys.model;

/**
 * The numbers shown at the top of the inventory page.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
public class InventorySummary {

    private int activeCount;         // not discontinued
    private int lowStockCount;
    private int outOfStockCount;
    private int expiredCount;
    private int discontinuedCount;

    public int getActiveCount() {
        return activeCount;
    }

    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(int lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public int getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(int outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public int getExpiredCount() {
        return expiredCount;
    }

    public void setExpiredCount(int expiredCount) {
        this.expiredCount = expiredCount;
    }

    public int getDiscontinuedCount() {
        return discontinuedCount;
    }

    public void setDiscontinuedCount(int discontinuedCount) {
        this.discontinuedCount = discontinuedCount;
    }
}
