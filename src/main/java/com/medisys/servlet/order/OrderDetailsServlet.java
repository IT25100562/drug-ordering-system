package com.medisys.servlet.order;

import com.medisys.service.DeliveryService;
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
 * One of the customer's orders: timeline, medicines, totals, payment and delivery.
 *
 *   GET /orders/view?id=12            (&placed=1 right after checkout)
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet("/orders/view")
public class OrderDetailsServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();
    private final DeliveryService deliveryService = new DeliveryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            if (id == null) {
                throw new ValidationException("Not found");
            }
            request.setAttribute("order", orderService.getOwnOrder(SessionUtil.currentUser(request), id));
            // Checked above that the order is theirs, so its delivery is theirs too (module 06).
            request.setAttribute("delivery", deliveryService.getForOrder(id));
            request.setAttribute("justPlaced", "1".equals(request.getParameter("placed")));
            request.getRequestDispatcher("/WEB-INF/views/order/order-details.jsp").forward(request, response);
        } catch (ValidationException e) {
            // Someone else's (or no such) order: do not reveal which.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException("Could not load the order", e);
        }
    }
}
