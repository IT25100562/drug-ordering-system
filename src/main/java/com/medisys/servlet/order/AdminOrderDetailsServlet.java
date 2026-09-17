package com.medisys.servlet.order;

import com.medisys.model.Order;
import com.medisys.service.DeliveryService;
import com.medisys.service.OrderService;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Admin view of one order, with the buttons to move it on or cancel it.
 *
 *   GET /admin/orders/view?id=12
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet("/admin/orders/view")
public class AdminOrderDetailsServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();
    private final DeliveryService deliveryService = new DeliveryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            Order order = id == null ? null : orderService.getOrder(id);
            if (order == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("order", order);
            request.setAttribute("delivery", deliveryService.getForOrder(id));
            request.getRequestDispatcher("/WEB-INF/views/order/admin-order-details.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load the order", e);
        }
    }
}
