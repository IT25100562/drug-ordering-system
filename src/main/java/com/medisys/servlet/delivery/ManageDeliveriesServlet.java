package com.medisys.servlet.delivery;

import com.medisys.dao.DeliveryDAO;
import com.medisys.model.User;
import com.medisys.service.DeliveryService;
import com.medisys.util.SessionUtil;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * List of deliveries in three tabs.
 *
 *   GET /staff/deliveries?tab=NEW
 *   (tab: NEW (default), ON_THE_WAY or COMPLETED)
 *
 * A rider (DELIVERY_STAFF) sees their own deliveries plus the new ones nobody
 * has taken yet. The admin can see all of them (read only).
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
@WebServlet("/staff/deliveries")
public class ManageDeliveriesServlet extends HttpServlet {

    private final DeliveryService deliveryService = new DeliveryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        String filter = TextUtil.clean(request.getParameter("tab")).toUpperCase();
        if (!List.of(DeliveryDAO.FILTER_ON_THE_WAY, DeliveryDAO.FILTER_COMPLETED).contains(filter)) {
            filter = DeliveryDAO.FILTER_NEW;
        }

        try {
            request.setAttribute("deliveries", deliveryService.getDeliveries(user, filter));
            request.setAttribute("counts", deliveryService.getCounts(user));
        } catch (SQLException e) {
            throw new ServletException("Could not load the deliveries", e);
        }
        request.setAttribute("filter", filter);
        request.getRequestDispatcher("/WEB-INF/views/delivery/manage-deliveries.jsp").forward(request, response);
    }
}
