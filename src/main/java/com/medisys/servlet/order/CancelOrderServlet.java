package com.medisys.servlet.order;

import com.medisys.model.Order;
import com.medisys.service.OrderService;
import com.medisys.service.ValidationException;
import com.medisys.util.SessionUtil;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Customer cancels an order that has not been packed yet.
 *
 *   POST /orders/cancel   id=12&reason=...
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet("/orders/cancel")
public class CancelOrderServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            if (id == null) {
                throw new ValidationException("That order was not found.");
            }
            Order order = orderService.cancelByCustomer(SessionUtil.currentUser(request), id,
                    request.getParameter("reason"));
            SessionUtil.flash(request, "success", "Order " + order.getReference() + " was cancelled and "
                    + TextUtil.money(order.getTotal()) + " was refunded to your card ending "
                    + order.getPayment().getCardLast4() + ".");
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not cancel the order", e);
        }
        response.sendRedirect(request.getContextPath()
                + (id == null ? "/orders" : "/orders/view?id=" + id));
    }
}
