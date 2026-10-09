package com.medisys.order;

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
import java.util.EnumSet;

/**
 * The admin's order pages.
 *
 *   GET  /admin/orders?status=OPEN&q=...           all orders (filter + search)    READ
 *   GET  /admin/orders/view?id=5                   one order                       READ
 *   POST /admin/orders/status  action=advance      mark a new order as packed      UPDATE
 *   POST /admin/orders/status  action=cancel       cancel with a reason (refund)   UPDATE
 *
 * The later steps (out for delivery, delivered) are done by the rider
 * (module 06), because the rider picks the parcel up and hands it over.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet({"/admin/orders", "/admin/orders/view", "/admin/orders/status"})
public class ManageOrdersServlet extends HttpServlet {

    private final OrderDAO orderDAO = new OrderDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    // ================================================================== READ

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/admin/orders/view")) {
                Integer id = TextUtil.parseInt(request.getParameter("id"));
                Order order = id == null ? null : orderDAO.getOrderById(id);
                if (order == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("order", order);
                request.setAttribute("delivery", deliveryDAO.getDeliveryByOrder(id));
                request.getRequestDispatcher("/WEB-INF/views/order/admin-order-details.jsp").forward(request, response);
                return;
            }

            String filter = TextUtil.clean(request.getParameter("status")).toUpperCase();
            if (OrderStatus.fromText(filter) == null && !OrderDAO.FILTER_ALL.equals(filter)) {
                filter = OrderDAO.FILTER_OPEN;
            }
            String keyword = TextUtil.clean(request.getParameter("q"));
            if (keyword.length() > 100) {
                keyword = keyword.substring(0, 100);
            }
            request.setAttribute("orders", orderDAO.getOrders(filter, keyword));
            request.setAttribute("counts", orderDAO.countByStatus());
            request.setAttribute("filter", filter);
            request.setAttribute("keyword", keyword);
            request.getRequestDispatcher("/WEB-INF/views/order/manage-orders.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load the orders", e);
        }
    }

    // ================================================================ UPDATE

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User admin = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        String action = TextUtil.clean(request.getParameter("action"));
        try {
            Order order = id == null ? null : orderDAO.getOrderById(id);
            if (order == null) {
                throw new ValidationException(id == null ? "No order was selected." : "That order no longer exists.");
            }
            String message;
            if ("advance".equals(action)) {
                message = markPacked(admin, order, request.getParameter("current"), request.getParameter("note"));
            } else if ("cancel".equals(action)) {
                message = cancel(admin, order, request.getParameter("reason"));
            } else {
                throw new ValidationException("Unknown action.");
            }
            SessionUtil.flash(request, "success", message);
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not update the order", e);
        }

        String back = request.getParameter("returnTo");
        if (!TextUtil.isSafeLocalPath(back)) {
            back = id == null ? "/admin/orders" : "/admin/orders/view?id=" + id;
        }
        response.sendRedirect(request.getContextPath() + back);
    }

    /**
     * Marks a new order as packed ("Being packed").
     *
     * @param currentText the status the admin saw on the page - stops a double
     *                    click from moving the order two steps
     */
    private String markPacked(User admin, Order order, String currentText, String noteText)
            throws SQLException, ValidationException {
        if (OrderStatus.fromText(currentText) != order.getStatus()) {
            throw new ValidationException(order.getReference() + " was already changed to \""
                    + order.getStatus().getLabel() + "\". Please check it again.");
        }
        OrderStatus next = order.getStatus().nextForPharmacy();
        if (next == null) {
            throw new ValidationException(order.getReference() + " is " + order.getStatus().getLabel()
                    + " and cannot be moved on here. Delivery staff update it from the Deliveries page.");
        }
        String note = TextUtil.clean(noteText);
        if (note.length() > 300) {
            throw new ValidationException("The note can have at most 300 characters.");
        }
        if (!orderDAO.moveToStatus(order.getId(), order.getStatus(), next, admin.getId(), note.isEmpty() ? null : note)) {
            throw new ValidationException(order.getReference() + " was just changed by someone else.");
        }

        String message = "Good news: order " + order.getReference() + " is being packed.";
        if (!note.isEmpty()) {
            message += " Note: " + note;
        }
        notificationDAO.addNotification(order.getUserId(), message, "/orders/view?id=" + order.getId());
        return order.getReference() + " is now \"" + next.getLabel() + "\". The customer has been notified.";
    }

    /** The pharmacy cancels an order that has not left yet. A reason is required. */
    private String cancel(User admin, Order order, String reasonText) throws SQLException, ValidationException {
        if (!order.getStatus().canBeCancelledByPharmacy()) {
            throw new ValidationException(order.getReference() + " is " + order.getStatus().getLabel()
                    + " and can no longer be cancelled.");
        }
        String reason = TextUtil.clean(reasonText);
        if (reason.length() < 5 || reason.length() > 300) {
            throw new ValidationException("Please write a reason for the customer (5 to 300 characters).");
        }
        if (!orderDAO.cancelOrder(order.getId(), EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING),
                admin.getId(), reason)) {
            throw new ValidationException(order.getReference() + " was just changed by someone else.");
        }
        String reasonWithStop = reason.matches(".*[.!?]$") ? reason : reason + ".";
        notificationDAO.addNotification(order.getUserId(), "Sorry, order " + order.getReference()
                + " was cancelled by the pharmacy: " + reasonWithStop + " Your payment of "
                + TextUtil.money(order.getTotal()) + " has been refunded." + OrderServlet.prescriptionNote(order),
                "/orders/view?id=" + order.getId());
        return order.getReference() + " was cancelled, the stock was put back and the payment refunded.";
    }
}
