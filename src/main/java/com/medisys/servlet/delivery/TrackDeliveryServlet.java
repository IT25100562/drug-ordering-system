package com.medisys.servlet.delivery;

import com.medisys.service.DeliveryService;
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
 * Customer tracks the delivery of one of their orders.
 *
 *   GET /deliveries/track?orderId=12
 *
 * Someone else's order (or a missing one) gives 404.
 * Without an orderId the customer is sent to their order list.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
@WebServlet("/deliveries/track")
public class TrackDeliveryServlet extends HttpServlet {

    private final DeliveryService deliveryService = new DeliveryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer orderId = TextUtil.parseInt(request.getParameter("orderId"));
        if (orderId == null) {
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }
        try {
            request.setAttribute("delivery",
                    deliveryService.getForCustomer(SessionUtil.currentUser(request), orderId));
            request.getRequestDispatcher("/WEB-INF/views/delivery/track.jsp").forward(request, response);
        } catch (ValidationException e) {
            // Do not reveal whether the order exists.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException("Could not load the delivery", e);
        }
    }
}
