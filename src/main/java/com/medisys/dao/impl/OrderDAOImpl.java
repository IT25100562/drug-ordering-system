package com.medisys.dao.impl;

import com.medisys.config.DBConnection;
import com.medisys.dao.DeliveryDAO;
import com.medisys.dao.OrderDAO;
import com.medisys.dao.StockShortageException;
import com.medisys.model.Order;
import com.medisys.model.OrderItem;
import com.medisys.model.OrderStatus;
import com.medisys.model.OrderStatusChange;
import com.medisys.model.Payment;
import com.medisys.model.Prescription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JDBC implementation of OrderDAO (SQL Server).
 *
 * Placing and cancelling an order touch several tables (orders, order_items,
 * payments, medicines, cart_items, prescriptions), so they run inside a
 * transaction: con.setAutoCommit(false) ... commit(), or rollback() on any
 * problem, so the database is never left half-updated.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
public class OrderDAOImpl implements OrderDAO {

    private final DeliveryDAO deliveryDAO = new DeliveryDAOImpl();

    private static final String SELECT_ORDER =
            "SELECT o.*, u.full_name AS customer_name, u.email AS customer_email, rx.id AS prescription_id "
            + "FROM orders o "
            + "JOIN users u ON u.id = o.user_id "
            + "LEFT JOIN prescriptions rx ON rx.order_id = o.id ";

    // ================================================================ create

    @Override
    public int create(Order order, Integer prescriptionId) throws SQLException, StockShortageException {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. A prescription must still be payable (locked until commit).
                if (prescriptionId != null && !lockPayablePrescription(con, prescriptionId, order.getUserId())) {
                    con.rollback();
                    return NOT_PAYABLE;
                }

                // 2. Take every medicine out of stock, only if there is enough.
                String stock = "UPDATE medicines SET stock_quantity = stock_quantity - ?, updated_at = SYSDATETIME() "
                             + "WHERE id = ? AND stock_quantity >= ? AND is_discontinued = 0";
                try (PreparedStatement ps = con.prepareStatement(stock)) {
                    for (OrderItem item : order.getItems()) {
                        ps.setInt(1, item.getQuantity());
                        ps.setInt(2, item.getMedicineId());
                        ps.setInt(3, item.getQuantity());
                        if (ps.executeUpdate() != 1) {
                            con.rollback();
                            throw new StockShortageException(item.getMedicineId());
                        }
                    }
                }

                // 3. The order itself.
                int orderId = insertOrder(con, order);

                // 4. Its lines.
                String line = "INSERT INTO order_items (order_id, medicine_id, medicine_name, dosage_form, "
                            + "unit_price, quantity, dosage_instructions) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(line)) {
                    for (OrderItem item : order.getItems()) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, item.getMedicineId());
                        ps.setString(3, item.getMedicineName());
                        ps.setString(4, item.getDosageForm());
                        ps.setBigDecimal(5, item.getUnitPrice());
                        ps.setInt(6, item.getQuantity());
                        ps.setString(7, item.getDosageInstructions());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // 5. The payment.
                Payment payment = order.getPayment();
                String pay = "INSERT INTO payments (order_id, amount, card_last4, reference) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(pay)) {
                    ps.setInt(1, orderId);
                    ps.setBigDecimal(2, payment.getAmount());
                    ps.setString(3, payment.getCardLast4());
                    ps.setString(4, payment.getReference());
                    ps.executeUpdate();
                }

                // 6. First step of the history.
                addHistory(con, orderId, OrderStatus.PAID, "Order placed and paid", null);

                // 6b. Its delivery (module 06), so every paid order can be tracked.
                deliveryDAO.createForOrder(con, orderId);

                // 7. Link the prescription, or take the bought medicines out of the cart.
                if (prescriptionId != null) {
                    String link = "UPDATE prescriptions SET order_id = ?, updated_at = SYSDATETIME() "
                                + "WHERE id = ? AND order_id IS NULL";
                    try (PreparedStatement ps = con.prepareStatement(link)) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, prescriptionId);
                        if (ps.executeUpdate() != 1) {
                            con.rollback();
                            return NOT_PAYABLE;
                        }
                    }
                } else {
                    // Only the lines that were bought: something added in another tab stays.
                    String marks = String.join(", ", Collections.nCopies(order.getItems().size(), "?"));
                    String clear = "DELETE FROM cart_items WHERE user_id = ? AND medicine_id IN (" + marks + ")";
                    try (PreparedStatement ps = con.prepareStatement(clear)) {
                        ps.setInt(1, order.getUserId());
                        for (int i = 0; i < order.getItems().size(); i++) {
                            ps.setInt(i + 2, order.getItems().get(i).getMedicineId());
                        }
                        ps.executeUpdate();
                    }
                }

                con.commit();
                return orderId;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    private boolean lockPayablePrescription(Connection con, int prescriptionId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM prescriptions WITH (UPDLOCK, ROWLOCK) "
                   + "WHERE id = ? AND user_id = ? AND status = 'APPROVED' AND order_id IS NULL "
                   + "AND uploaded_at >= DATEADD(day, -" + Prescription.EXPIRY_DAYS + ", SYSDATETIME())";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, prescriptionId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 1;
            }
        }
    }

    private int insertOrder(Connection con, Order o) throws SQLException {
        String sql = "INSERT INTO orders (user_id, source, subtotal, delivery_fee, total, delivery_name, "
                   + "delivery_address, delivery_phone, delivery_note) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getUserId());
            ps.setString(2, o.getSource());
            ps.setBigDecimal(3, o.getSubtotal());
            ps.setBigDecimal(4, o.getDeliveryFee());
            ps.setBigDecimal(5, o.getTotal());
            ps.setString(6, o.getDeliveryName());
            ps.setString(7, o.getDeliveryAddress());
            ps.setString(8, o.getDeliveryPhone());
            ps.setString(9, o.getDeliveryNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void addHistory(Connection con, int orderId, OrderStatus status, String note, Integer changedBy)
            throws SQLException {
        String sql = "INSERT INTO order_status_history (order_id, status, note, changed_by) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, status.name());
            ps.setString(3, note);
            ps.setObject(4, changedBy);
            ps.executeUpdate();
        }
    }

    // ================================================================= read

    @Override
    public Order findById(int id) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            List<Order> list = query(con, SELECT_ORDER + "WHERE o.id = ?", id);
            if (list.isEmpty()) {
                return null;
            }
            Order order = list.get(0);
            loadHistory(con, order);
            return order;
        }
    }

    @Override
    public List<Order> findByUser(int userId) throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            return query(con, SELECT_ORDER + "WHERE o.user_id = ? ORDER BY o.created_at DESC, o.id DESC", userId);
        }
    }

    @Override
    public List<Order> findForAdmin(String filter, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ORDER).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        OrderStatus status = OrderStatus.fromText(filter);
        if (status != null) {
            sql.append("AND o.status = ? ");
            params.add(status.name());
        } else if (FILTER_OPEN.equals(filter)) {
            sql.append("AND o.status IN ('PAID', 'PROCESSING', 'SHIPPED') ");
        }

        if (keyword != null && !keyword.isBlank()) {
            // "ORD-000012", "12" or part of the customer's name / email
            String digits = keyword.replaceAll("(?i)^ord-?", "").replaceFirst("^0+(?=\\d)", "");
            sql.append("AND (u.full_name LIKE ? OR u.email LIKE ?");
            String pattern = "%" + keyword.trim().replace("[", "[[]").replace("%", "[%]").replace("_", "[_]") + "%";
            params.add(pattern);
            params.add(pattern);
            if (digits.matches("\\d{1,9}")) {
                sql.append(" OR o.id = ?");
                params.add(Integer.parseInt(digits));
            }
            sql.append(") ");
        }
        sql.append("ORDER BY o.created_at DESC, o.id DESC");

        try (Connection con = DBConnection.getInstance().getConnection()) {
            return query(con, sql.toString(), params.toArray());
        }
    }

    @Override
    public Map<String, Integer> countByStatus() throws SQLException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            counts.put(s.name(), 0);
        }
        int open = 0;
        int all = 0;
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT status, COUNT(*) FROM orders GROUP BY status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String status = rs.getString(1);
                int n = rs.getInt(2);
                counts.put(status, n);
                all += n;
                if (!status.equals("DELIVERED") && !status.equals("CANCELLED")) {
                    open += n;
                }
            }
        }
        counts.put(FILTER_OPEN, open);
        counts.put(FILTER_ALL, all);
        return counts;
    }

    // =============================================================== update

    @Override
    public boolean advance(int id, OrderStatus from, OrderStatus to, Integer changedBy, String note)
            throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                String sql = "UPDATE orders SET status = ?, updated_at = SYSDATETIME() WHERE id = ? AND status = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, to.name());
                    ps.setInt(2, id);
                    ps.setString(3, from.name());
                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }
                addHistory(con, id, to, note, changedBy);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    @Override
    public boolean cancel(int id, Set<OrderStatus> allowedFrom, Integer changedBy, String reason)
            throws SQLException {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. Lock the order and check its status.
                OrderStatus current = null;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT status FROM orders WITH (UPDLOCK, ROWLOCK) WHERE id = ?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            current = OrderStatus.fromText(rs.getString(1));
                        }
                    }
                }
                if (current == null || !allowedFrom.contains(current)) {
                    con.rollback();
                    return false;
                }

                // 2. Cancel it.
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE orders SET status = 'CANCELLED', cancel_reason = ?, updated_at = SYSDATETIME() WHERE id = ?")) {
                    ps.setString(1, reason);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }

                // 3. Put the medicines back in stock.
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE m SET m.stock_quantity = m.stock_quantity + i.quantity, m.updated_at = SYSDATETIME() "
                        + "FROM medicines m JOIN order_items i ON i.medicine_id = m.id WHERE i.order_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                // 4. Refund the (test) payment.
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE payments SET status = 'REFUNDED', refunded_at = SYSDATETIME() WHERE order_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                // 5. A prescription paid by this order can be paid again.
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE prescriptions SET order_id = NULL, updated_at = SYSDATETIME() WHERE order_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                // 6. The delivery is cancelled too (module 06).
                deliveryDAO.cancelForOrder(con, id, reason, changedBy);

                addHistory(con, id, OrderStatus.CANCELLED, reason, changedBy);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // ============================================================= helpers

    /** Runs an order query, then loads the lines and payments of all results. */
    private List<Order> query(Connection con, String sql, Object... params) throws SQLException {
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        }
        if (list.isEmpty()) {
            return list;
        }

        Map<Integer, Order> byId = new HashMap<>();
        for (Order o : list) {
            byId.put(o.getId(), o);
        }
        String marks = String.join(", ", Collections.nCopies(list.size(), "?"));

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM order_items WHERE order_id IN (" + marks + ") ORDER BY id")) {
            setIds(ps, list);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getInt("id"));
                    item.setMedicineId(rs.getInt("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setDosageForm(rs.getString("dosage_form"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setDosageInstructions(rs.getString("dosage_instructions"));
                    byId.get(rs.getInt("order_id")).getItems().add(item);
                }
            }
        }

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM payments WHERE order_id IN (" + marks + ")")) {
            setIds(ps, list);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getInt("id"));
                    p.setOrderId(rs.getInt("order_id"));
                    p.setAmount(rs.getBigDecimal("amount"));
                    p.setMethod(rs.getString("method"));
                    p.setCardLast4(rs.getString("card_last4"));
                    p.setReference(rs.getString("reference"));
                    p.setStatus(rs.getString("status"));
                    p.setPaidAt(toTime(rs.getTimestamp("paid_at")));
                    p.setRefundedAt(toTime(rs.getTimestamp("refunded_at")));
                    byId.get(p.getOrderId()).setPayment(p);
                }
            }
        }
        return list;
    }

    private void loadHistory(Connection con, Order order) throws SQLException {
        String sql = "SELECT h.status, h.note, h.changed_at, u.full_name FROM order_status_history h "
                   + "LEFT JOIN users u ON u.id = h.changed_by WHERE h.order_id = ? ORDER BY h.changed_at, h.id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, order.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderStatusChange change = new OrderStatusChange();
                    change.setStatus(OrderStatus.fromText(rs.getString("status")));
                    change.setNote(rs.getString("note"));
                    change.setChangedAt(toTime(rs.getTimestamp("changed_at")));
                    change.setChangedByName(rs.getString("full_name"));
                    order.getHistory().add(change);
                }
            }
        }
    }

    private static void setIds(PreparedStatement ps, List<Order> list) throws SQLException {
        for (int i = 0; i < list.size(); i++) {
            ps.setInt(i + 1, list.get(i).getId());
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setUserId(rs.getInt("user_id"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setCustomerEmail(rs.getString("customer_email"));
        o.setSource(rs.getString("source"));
        o.setPrescriptionId((Integer) rs.getObject("prescription_id"));
        o.setStatus(OrderStatus.fromText(rs.getString("status")));
        o.setSubtotal(rs.getBigDecimal("subtotal"));
        o.setDeliveryFee(rs.getBigDecimal("delivery_fee"));
        o.setTotal(rs.getBigDecimal("total"));
        o.setDeliveryName(rs.getString("delivery_name"));
        o.setDeliveryAddress(rs.getString("delivery_address"));
        o.setDeliveryPhone(rs.getString("delivery_phone"));
        o.setDeliveryNote(rs.getString("delivery_note"));
        o.setCancelReason(rs.getString("cancel_reason"));
        o.setCreatedAt(toTime(rs.getTimestamp("created_at")));
        o.setUpdatedAt(toTime(rs.getTimestamp("updated_at")));
        return o;
    }

    private static LocalDateTime toTime(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }
}
