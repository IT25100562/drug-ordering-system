package com.medisys.servlet.delivery;

import com.medisys.model.Delivery;
import com.medisys.model.User;
import com.medisys.service.DeliveryService;
import com.medisys.service.OrderService;
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
 * One delivery for the rider or the admin: address, medicines, history and
 * the buttons to move it on.
 *
 *   GET /staff/deliveries/view?id=3
 *
 * A rider gets 404 for a delivery that is not theirs.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
@WebServlet("/staff/deliveries/view")
public class DeliveryDetailsServlet extends HttpServlet {

    private final DeliveryService deliveryService = new DeliveryService();
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            Delivery delivery = id == null ? null : deliveryService.getForStaff(user, id);
            if (delivery == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("delivery", delivery);
            // The medicines to hand over (from module 02's order).
            request.setAttribute("order", orderService.getOrder(delivery.getOrderId()));
            if (user.isAdmin()) {
                request.setAttribute("riders", deliveryService.getRiders());
            }
            request.getRequestDispatcher("/WEB-INF/views/delivery/delivery-details.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load the delivery", e);
        }
    }
}
