package com.medisys.dao;

import com.medisys.model.Delivery;
import com.medisys.model.DeliveryStatus;

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

    /** The three tabs of the deliveries page. */
    String FILTER_NEW = "NEW";                  // at the pharmacy (PENDING), waiting to be picked up
    String FILTER_ON_THE_WAY = "ON_THE_WAY";    // picked up, not delivered yet (incl. failed attempts)
    String FILTER_COMPLETED = "COMPLETED";      // delivered or cancelled

    /** Extra counts (the menu badge in header.jspf uses them). */
    String FILTER_ACTIVE = "ACTIVE";            // everything not delivered or cancelled
    String FILTER_UNASSIGNED = "UNASSIGNED";    // at the pharmacy, no rider has taken it yet
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
     * @param staffId a rider: their own deliveries plus the new ones nobody has
     *                picked up yet; null for all (admin)
     * @param filter  FILTER_NEW, FILTER_ON_THE_WAY or FILTER_COMPLETED
     */
    List<Delivery> findForStaff(Integer staffId, String filter) throws SQLException;

    /**
     * Number of deliveries per status name and per FILTER_ value, counting the
     * same deliveries findForStaff shows.
     */
    Map<String, Integer> countByStatus(Integer staffId) throws SQLException;

    /**
     * The rider takes the parcel from the pharmacy, in ONE transaction:
     *  - the delivery gets this rider (only if nobody else took it first)
     *    and goes PENDING -> OUT_FOR_DELIVERY (the first attempt)
     *  - the order goes (PAID ->) PROCESSING -> SHIPPED (with order history rows)
     *  - two update rows for the tracking page (Dispatched, Out for delivery)
     *
     * @return false if the delivery or the order was not in the expected state,
     *         or another rider took it first
     */
    boolean pickUp(Delivery delivery, int riderId, String riderName, int changedBy) throws SQLException;

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
