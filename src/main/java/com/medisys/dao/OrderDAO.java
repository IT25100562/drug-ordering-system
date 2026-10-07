package com.medisys.dao;

import com.medisys.model.Order;
import com.medisys.model.OrderStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Database operations for orders, their lines, payments and status history.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
public interface OrderDAO {

    /** create() result when the prescription can no longer be paid. */
    int NOT_PAYABLE = -1;

    /** Admin list filters, besides the status names. */
    String FILTER_OPEN = "OPEN";        // PAID + PROCESSING + SHIPPED
    String FILTER_ALL = "ALL";

    /**
     * Saves a new paid order in ONE transaction:
     *  - takes every medicine out of stock (throws StockShortageException if one is short)
     *  - saves the order, its lines, the payment and the first history step
     *  - for a prescription order: links the prescription (it must still be
     *    approved, unpaid and not expired, otherwise NOT_PAYABLE is returned)
     *  - for a cart order: removes the bought medicines from the cart
     * If anything fails, nothing is saved.
     *
     * @param order          the order with its items and payment filled in
     * @param prescriptionId the prescription being paid, or null for a cart order
     * @return the new order id, or NOT_PAYABLE
     */
    int create(Order order, Integer prescriptionId) throws SQLException, StockShortageException;

    /** The order with items, payment and history, or null. */
    Order findById(int id) throws SQLException;

    /** The customer's orders, newest first (with items and payment). */
    List<Order> findByUser(int userId) throws SQLException;

    /**
     * Orders for the admin page, newest first.
     *
     * @param filter  a status name, FILTER_OPEN or FILTER_ALL
     * @param keyword order number, customer name or email (may be empty)
     */
    List<Order> findForAdmin(String filter, String keyword) throws SQLException;

    /** Number of orders per status name, plus FILTER_OPEN and FILTER_ALL. */
    Map<String, Integer> countByStatus() throws SQLException;

    /** Moves an order one step on, only if it is still in "from". */
    boolean advance(int id, OrderStatus from, OrderStatus to, Integer changedBy, String note) throws SQLException;

    /**
     * Cancels an order in ONE transaction: puts the medicines back in stock,
     * marks the payment refunded, and frees a paid prescription so it can be
     * paid again. Only if the order's status is one of "allowedFrom".
     */
    boolean cancel(int id, Set<OrderStatus> allowedFrom, Integer changedBy, String reason) throws SQLException;
}
