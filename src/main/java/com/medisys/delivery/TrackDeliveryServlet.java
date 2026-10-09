package com.medisys.delivery;

import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * The customer tracks the delivery of their own order (READ).
 *
 *   GET /deliveries/track?orderId=5
 *
 * Someone else's order answers 404, so nobody can tell whether it exists.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
@WebServlet("/deliveries/track")
public class TrackDeliveryServlet extends HttpServlet {

    private final DeliveryDAO deliveryDAO = new DeliveryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer orderId = TextUtil.parseInt(request.getParameter("orderId"));
        if (orderId == null) {
            response.sendRedirect(request.getContextPath() + "/orders");
            return;
        }
        try {
            Delivery delivery = deliveryDAO.getDeliveryByOrder(orderId);
            if (delivery == null || delivery.getCustomerId() != SessionUtil.currentUser(request).getId()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("delivery", delivery);
            request.getRequestDispatcher("/WEB-INF/views/delivery/track.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load the delivery", e);
        }
    }
}
