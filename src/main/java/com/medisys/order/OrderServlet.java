package com.medisys.order;

import com.medisys.cart.CartServlet;
import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.ValidationException;
import com.medisys.delivery.DeliveryDAO;
import com.medisys.delivery.NotificationDAO;
import com.medisys.user.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The customer's own orders.
 *
 *   GET  /orders                 my orders, newest first                        READ
 *   GET  /orders/view?id=5       one order with its timeline and delivery       READ
 *   POST /orders/cancel          cancel before packing starts (refund)          UPDATE
 *   POST /orders/reorder         put the same medicines in the cart again
 *
 * A customer can only see their own orders: someone else's order answers
 * 404, so nobody can even tell that it exists.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet({"/orders", "/orders/view", "/orders/cancel", "/orders/reorder"})
public class OrderServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    // ================================================================== READ

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            if (request.getServletPath().equals("/orders")) {
                request.setAttribute("orders", orderDAO.getOrdersByUser(user.getId()));
                request.getRequestDispatcher("/WEB-INF/views/order/my-orders.jsp").forward(request, response);
                return;
            }
            Order order = findOwnOrder(request, user);
            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("order", order);
            request.setAttribute("delivery", deliveryDAO.getDeliveryByOrder(order.getId()));
            request.setAttribute("justPlaced", "1".equals(request.getParameter("placed")));
            request.getRequestDispatcher("/WEB-INF/views/order/order-details.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load your orders", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        String back = id == null ? "/orders" : "/orders/view?id=" + id;
        try {
            Order order = findOwnOrder(request, user);
            if (order == null) {
                throw new ValidationException("That order was not found.");
            }
            if (request.getServletPath().equals("/orders/cancel")) {
                cancel(order, request.getParameter("reason"));
                SessionUtil.flash(request, "success", "Order " + order.getReference() + " was cancelled and "
                        + TextUtil.money(order.getTotal()) + " was refunded to your card ending "
                        + order.getPayment().getCardLast4() + ".");
            } else {
                SessionUtil.flash(request, "success", reorder(order, user));
                back = "/cart";
            }
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not update the order", e);
        }
        response.sendRedirect(request.getContextPath() + back);
    }

    // ================================================================ UPDATE

    /** The customer cancels an order that has not been packed yet. */
    private void cancel(Order order, String reasonText) throws SQLException, ValidationException {
        if (!order.getStatus().canBeCancelledByCustomer()) {
            throw new ValidationException(order.getReference() + " can no longer be cancelled ("
                    + order.getStatus().getLabel() + "). Please contact the pharmacy.");
        }
        String reason = TextUtil.clean(reasonText);
        if (reason.length() > 300) {
            reason = reason.substring(0, 300);
        }
        String note = "Cancelled by the customer" + (reason.isEmpty() ? "" : ": " + reason);
        if (!orderDAO.cancelOrder(order.getId(), EnumSet.of(OrderStatus.PAID), null, note)) {
            throw new ValidationException(order.getReference() + " is already being packed and can no longer "
                    + "be cancelled. Please contact the pharmacy.");
        }
        notificationDAO.addNotification(order.getUserId(), "Order " + order.getReference() + " was cancelled. "
                + "Your payment of " + TextUtil.money(order.getTotal()) + " has been refunded."
                + prescriptionNote(order), "/orders/view?id=" + order.getId());
    }

    /**
     * Puts the medicines of an old order into the cart again, with the normal
     * cart rules. Prescription-only and unavailable medicines are skipped.
     */
    private String reorder(Order order, User customer) throws SQLException, ValidationException {
        int added = 0;
        List<String> skipped = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            try {
                CartServlet.addToCart(customer.getId(), item.getMedicineId(), String.valueOf(item.getQuantity()));
                added++;
            } catch (ValidationException e) {
                skipped.add(item.getMedicineName());
            }
        }
        if (added == 0) {
            throw new ValidationException("None of these medicines can be added to the cart right now"
                    + (order.isFromPrescription() ? " (prescription medicines need a new prescription)." : "."));
        }
        return added + " item" + (added == 1 ? " was" : "s were") + " added to your cart."
                + (skipped.isEmpty() ? "" : " Not added: " + String.join(", ", skipped) + ".");
    }

    // =============================================================== helpers

    /** The customer's own order from the "id" parameter, or null (also for someone else's order). */
    private Order findOwnOrder(HttpServletRequest request, User customer) throws SQLException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Order order = id == null ? null : orderDAO.getOrderById(id);
        return order == null || order.getUserId() != customer.getId() ? null : order;
    }

    /** Used by ManageOrdersServlet too. */
    static String prescriptionNote(Order order) {
        return order.isFromPrescription()
                ? " You can pay for prescription " + order.getPrescriptionReference() + " again while it is valid."
                : "";
    }
}
