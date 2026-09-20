package com.medisys.dao.impl;

import com.medisys.config.DBConnection;
import com.medisys.dao.DeliveryDAO;
import com.medisys.model.Delivery;
import com.medisys.model.DeliveryStatus;
import com.medisys.model.DeliveryUpdate;
import com.medisys.model.OrderStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of DeliveryDAO (SQL Server).
 *
 * A status change touches the deliveries, delivery_updates, orders and
 * order_status_history tables, so it runs inside a transaction:
 * con.setAutoCommit(false) ... commit(), or rollback() on any problem.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
public class DeliveryDAOImpl implements DeliveryDAO {

    /** A new order is expected to arrive this many days after it was placed. */
    public static final int DELIVERY_DAYS = 2;

    // The delivery, its rider (LEFT JOIN: there may be none yet), and the
    // order's delivery details and customer.
    private static final String SELECT_DELIVERY =
            "SELECT d.*, s.full_name AS staff_name, s.phone AS staff_phone, "
            + "o.user_id AS customer_id, c.full_name AS customer_name, o.status AS order_status, "
            + "o.delivery_name, o.delivery_address, o.delivery_phone, o.delivery_note "
            + "FROM deliveries d "
            + "JOIN orders o ON o.id = d.order_id "
            + "JOIN users c ON c.id = o.user_id "
            + "LEFT JOIN users s ON s.id = d.staff_id ";

    // What a rider sees: their own deliveries, plus the new ones at the
    // pharmacy that nobody has picked up yet (any rider may take those).
    private static final String RIDER_SEES =
            "AND (d.staff_id = ? OR (d.staff_id IS NULL AND d.status = 'PENDING')) ";

    // ======================================================= create / cancel

    @Override
    public void createForOrder(Connection con, int orderId) throws SQLException {
        String sql = "INSERT INTO deliveries (order_id, estimated_date) "
                   + "VALUES (?, CAST(DATEADD(day, ?, SYSDATETIME()) AS DATE))";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, DELIVERY_DAYS);
            ps.executeUpdate();
        }
        addUpdateForOrder(con, orderId, "Order received. We are preparing your parcel.", null);
    }

    @Override
    public void cancelForOrder(Connection con, int orderId, String reason, Integer changedBy) throws SQLException {
        String sql = "UPDATE deliveries SET status = 'CANCELLED', updated_at = SYSDATETIME() "
                   + "WHERE order_id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            if (ps.executeUpdate() == 1) {
                addUpdateForOrder(con, orderId, reason, changedBy);
            }
        }
    }

    /** Adds an update row with the delivery's current status (found by its order). */
    private void addUpdateForOrder(Connection con, int orderId, String note, Integer changedBy) throws SQLException {
        String sql = "INSERT INTO delivery_updates (delivery_id, status, note, updated_by) "
                   + "SELECT id, status, ?, ? FROM deliveries WHERE order_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, note);
            ps.setObject(2, changedBy);
            ps.setInt(3, orderId);
            ps.executeUpdate();
        }
    }

    private void addUpdate(Connection con, int deliveryId, DeliveryStatus status, String note, Integer changedBy)
            throws SQLException {
        String sql = "INSERT INTO delivery_updates (delivery_id, status, note, updated_by) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, deliveryId);
            ps.setString(2, status.name());
            ps.setString(3, note);
            ps.setObject(4, changedBy);
            ps.executeUpdate();
        }
    }

    // ================================================================= read

    @Override
    public Delivery findById(int id) throws SQLException {
        return findOne("WHERE d.id = ?", id);
    }

    @Override
    public Delivery findByOrder(int orderId) throws SQLException {
        return findOne("WHERE d.order_id = ?", orderId);
    }

    private Delivery findOne(String where, int value) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            List<Delivery> list = query(con, SELECT_DELIVERY + where, value);
            if (list.isEmpty()) {
                return null;
            }
            Delivery delivery = list.get(0);
            loadUpdates(con, delivery);
            return delivery;
        }
    }

    @Override
    public List<Delivery> findForStaff(Integer staffId, String filter) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_DELIVERY).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (staffId != null) {
            sql.append(RIDER_SEES);
            params.add(staffId);
        }

        if (FILTER_NEW.equals(filter)) {
            sql.append("AND d.status = 'PENDING' ");
        } else if (FILTER_ON_THE_WAY.equals(filter)) {
            sql.append("AND d.status IN ('DISPATCHED', 'OUT_FOR_DELIVERY', 'FAILED') ");
        } else if (FILTER_COMPLETED.equals(filter)) {
            sql.append("AND d.status IN ('DELIVERED', 'CANCELLED') ");
        }

        // Open deliveries first, the ones due soonest on top.
        // Finished ones after them, the newest on top.
        sql.append("ORDER BY CASE WHEN d.status IN ('DELIVERED', 'CANCELLED') THEN 1 ELSE 0 END, ")
           .append("CASE WHEN d.status IN ('DELIVERED', 'CANCELLED') THEN NULL ELSE d.estimated_date END, ")
           .append("d.updated_at DESC, d.id DESC");

        try (Connection con = DBConnection.getInstance().getConnection()) {
            return query(con, sql.toString(), params.toArray());
        }
    }

    @Override
    public Map<String, Integer> countByStatus(Integer staffId) throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (DeliveryStatus s : DeliveryStatus.values()) {
            counts.put(s.name(), 0);
        }
        int active = 0;
        int unassigned = 0;
        int all = 0;

        String sql = "SELECT d.status, CASE WHEN d.staff_id IS NULL THEN 1 ELSE 0 END AS no_rider, COUNT(*) AS n "
                   + "FROM deliveries d WHERE 1 = 1 " + (staffId == null ? "" : RIDER_SEES)
                   + "GROUP BY d.status, CASE WHEN d.staff_id IS NULL THEN 1 ELSE 0 END";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (staffId != null) {
                ps.setInt(1, staffId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    int n = rs.getInt("n");
                    counts.put(status, counts.get(status) + n);
                    all += n;
                    if (!DeliveryStatus.fromText(status).isFinished()) {
                        active += n;
                    }
                    if (status.equals("PENDING") && rs.getInt("no_rider") == 1) {
                        unassigned += n;
                    }
                }
            }
        }
        counts.put(FILTER_ACTIVE, active);
        counts.put(FILTER_UNASSIGNED, unassigned);
        counts.put(FILTER_ALL, all);
        counts.put(FILTER_NEW, counts.get("PENDING"));
        counts.put(FILTER_ON_THE_WAY, counts.get("DISPATCHED") + counts.get("OUT_FOR_DELIVERY") + counts.get("FAILED"));
        counts.put(FILTER_COMPLETED, counts.get("DELIVERED") + counts.get("CANCELLED"));
        return counts;
    }

    // =============================================================== update

    @Override
    public boolean pickUp(Delivery delivery, int riderId, String riderName, int changedBy) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. The delivery: still at the pharmacy, and free or already this rider's.
                //    If two riders press the button together, only one gets it.
                String sql = "UPDATE deliveries SET staff_id = ?, status = 'OUT_FOR_DELIVERY', "
                           + "attempts = attempts + 1, updated_at = SYSDATETIME() "
                           + "WHERE id = ? AND status = 'PENDING' AND (staff_id IS NULL OR staff_id = ?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, riderId);
                    ps.setInt(2, delivery.getId());
                    ps.setInt(3, riderId);
                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }

                // 2. The order leaves the pharmacy. The rider packs it up when
                //    collecting it, so a PAID order passes PROCESSING first; one the
                //    pharmacy already marked packed is PROCESSING already (moveOrder
                //    then returns false, which is fine). This keeps every step in the
                //    customer's order timeline.
                moveOrder(con, delivery.getOrderId(), OrderStatus.PAID, OrderStatus.PROCESSING,
                          "Packed and collected by " + riderName, changedBy);
                if (!moveOrder(con, delivery.getOrderId(), OrderStatus.PROCESSING, OrderStatus.SHIPPED,
                               "Picked up by " + riderName, changedBy)) {
                    con.rollback();
                    return false;
                }

                // 3. The tracking history keeps both steps, so the customer's
                //    progress bar shows the time of each one.
                addUpdate(con, delivery.getId(), DeliveryStatus.DISPATCHED, "Picked up by " + riderName, changedBy);
                addUpdate(con, delivery.getId(), DeliveryStatus.OUT_FOR_DELIVERY, null, changedBy);

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    @Override
    public boolean updateStatus(Delivery delivery, DeliveryStatus from, DeliveryStatus to, int changedBy,
                                String note) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. The delivery, only if nobody changed it in the meantime.
                //    A rider is required once the parcel leaves (also a CHECK in the table).
                String sql = "UPDATE deliveries SET status = ?, "
                           + "attempts = attempts + ?, "
                           + "delivered_at = CASE WHEN ? = 'DELIVERED' THEN SYSDATETIME() ELSE delivered_at END, "
                           + "updated_at = SYSDATETIME() "
                           + "WHERE id = ? AND status = ? AND staff_id IS NOT NULL";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, to.name());
                    ps.setInt(2, to == DeliveryStatus.OUT_FOR_DELIVERY ? 1 : 0);
                    ps.setString(3, to.name());
                    ps.setInt(4, delivery.getId());
                    ps.setString(5, from.name());
                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }

                // 2. Keep the order's status in step with the delivery.
                if (to == DeliveryStatus.DISPATCHED
                        && !moveOrder(con, delivery.getOrderId(), OrderStatus.PROCESSING, OrderStatus.SHIPPED,
                                      "Picked up by " + delivery.getStaffName(), changedBy)) {
                    con.rollback();
                    return false;
                }
                if (to == DeliveryStatus.DELIVERED
                        && !moveOrder(con, delivery.getOrderId(), OrderStatus.SHIPPED, OrderStatus.DELIVERED,
                                      note, changedBy)) {
                    con.rollback();
                    return false;
                }

                // 3. The tracking history.
                addUpdate(con, delivery.getId(), to, note, changedBy);

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Moves the order one step and adds its history row (module 02's tables). */
    private boolean moveOrder(Connection con, int orderId, OrderStatus from, OrderStatus to, String note,
                              int changedBy) throws SQLException {
        String sql = "UPDATE orders SET status = ?, updated_at = SYSDATETIME() WHERE id = ? AND status = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, to.name());
            ps.setInt(2, orderId);
            ps.setString(3, from.name());
            if (ps.executeUpdate() != 1) {
                return false;
            }
        }
        String history = "INSERT INTO order_status_history (order_id, status, note, changed_by) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(history)) {
            ps.setInt(1, orderId);
            ps.setString(2, to.name());
            ps.setString(3, note);
            ps.setInt(4, changedBy);
            ps.executeUpdate();
        }
        return true;
    }

    // ============================================================= helpers

    private List<Delivery> query(Connection con, String sql, Object... params) throws SQLException {
        List<Delivery> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDelivery(rs));
                }
            }
        }
        return list;
    }

    private void loadUpdates(Connection con, Delivery delivery) throws SQLException {
        String sql = "SELECT du.status, du.note, du.created_at, u.full_name FROM delivery_updates du "
                   + "LEFT JOIN users u ON u.id = du.updated_by "
                   + "WHERE du.delivery_id = ? ORDER BY du.created_at, du.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delivery.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DeliveryUpdate update = new DeliveryUpdate();
                    update.setStatus(DeliveryStatus.fromText(rs.getString("status")));
                    update.setNote(rs.getString("note"));
                    update.setCreatedAt(toTime(rs.getTimestamp("created_at")));
                    update.setUpdatedByName(rs.getString("full_name"));
                    delivery.getUpdates().add(update);
                }
            }
        }
    }

    private Delivery mapDelivery(ResultSet rs) throws SQLException {
        Delivery d = new Delivery();
        d.setId(rs.getInt("id"));
        d.setOrderId(rs.getInt("order_id"));
        d.setStaffId((Integer) rs.getObject("staff_id"));
        d.setStatus(DeliveryStatus.fromText(rs.getString("status")));
        d.setAttempts(rs.getInt("attempts"));
        Date estimated = rs.getDate("estimated_date");
        d.setEstimatedDate(estimated == null ? null : estimated.toLocalDate());
        d.setDeliveredAt(toTime(rs.getTimestamp("delivered_at")));
        d.setCreatedAt(toTime(rs.getTimestamp("created_at")));
        d.setUpdatedAt(toTime(rs.getTimestamp("updated_at")));
        d.setStaffName(rs.getString("staff_name"));
        d.setStaffPhone(rs.getString("staff_phone"));
        d.setCustomerId(rs.getInt("customer_id"));
        d.setCustomerName(rs.getString("customer_name"));
        d.setOrderStatus(OrderStatus.fromText(rs.getString("order_status")));
        d.setDeliveryName(rs.getString("delivery_name"));
        d.setDeliveryAddress(rs.getString("delivery_address"));
        d.setDeliveryPhone(rs.getString("delivery_phone"));
        d.setDeliveryNote(rs.getString("delivery_note"));
        return d;
    }

    private static LocalDateTime toTime(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
