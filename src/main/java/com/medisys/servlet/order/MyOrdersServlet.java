package com.medisys.servlet.order;

import com.medisys.service.OrderService;
import com.medisys.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Customer's order history.
 *
 *   GET /orders
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet("/orders")
public class MyOrdersServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("orders", orderService.getMyOrders(SessionUtil.currentUser(request).getId()));
        } catch (SQLException e) {
            throw new ServletException("Could not load your orders", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/order/my-orders.jsp").forward(request, response);
    }
}
