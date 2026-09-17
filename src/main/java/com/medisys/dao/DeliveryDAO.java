package com.medisys.dao;

import com.medisys.model.Delivery;
import com.medisys.model.DeliveryStatus;
import com.medisys.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Database operations for deliveries and their tracking updates.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
public interface DeliveryDAO {

    /** List filters, besides the status names. */
    String FILTER_ACTIVE = "ACTIVE";            // everything not delivered or cancelled
    String FILTER_UNASSIGNED = "UNASSIGNED";    // waiting for the admin to pick a rider
    String FILTER_ALL = "ALL";

    /**
     * Adds the delivery of a new order. Uses the caller's connection, so it is
     * part of the order's transaction (OrderDAOImpl.create): no paid order can
     * exist without a delivery.
     */
    void createForOrder(Connection con, int orderId) throws SQLException;

    /**
     * Cancels the delivery of a cancelled order (only while it is still
     * PENDING). Uses the caller's connection, like createForOrder.
     */
    void cancelForOrder(Connection con, int orderId, String reason, Integer changedBy) throws SQLException;

    /** The delivery with its updates, or null. */
    Delivery findById(int id) throws SQLException;

    /** The delivery of an order, with its updates, or null. */
    Delivery findByOrder(int orderId) throws SQLException;

    /**
     * Deliveries for the staff page (without their updates).
     *
     * @param staffId only this rider's deliveries, or null for all (admin)
     * @param filter  a status name, FILTER_ACTIVE, FILTER_UNASSIGNED or FILTER_ALL
     */
    List<Delivery> findForStaff(Integer staffId, String filter) throws SQLException;

    /** Number of deliveries per status name, plus the three FILTER_ values. */
    Map<String, Integer> countByStatus(Integer staffId) throws SQLException;

    /** Active delivery staff accounts, for the "assign rider" list. */
    List<User> findRiders() throws SQLException;

    /**
     * Gives the delivery to a rider, only while it may still be assigned
     * (PENDING or FAILED). Also saves an update row.
     */
    boolean assignStaff(int id, int staffId, int changedBy, String note) throws SQLException;

    /**
     * Moves a delivery from "from" to "to" in ONE transaction:
     *  - the delivery itself (only if it is still in "from")
     *  - OUT_FOR_DELIVERY counts one more attempt, DELIVERED saves the time
     *  - DISPATCHED moves the order PROCESSING -> SHIPPED,
     *    DELIVERED moves the order SHIPPED -> DELIVERED (with an order history row)
     *  - an update row for the tracking page
     *
     * @return false if the delivery or the order was not in the expected state
     */
    boolean updateStatus(Delivery delivery, DeliveryStatus from, DeliveryStatus to, int changedBy, String note)
            throws SQLException;
}
