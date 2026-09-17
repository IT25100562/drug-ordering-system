package com.medisys.servlet.delivery;

import com.medisys.dao.DeliveryDAO;
import com.medisys.model.DeliveryStatus;
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

/**
 * List of deliveries with status tabs.
 *
 *   GET /staff/deliveries?status=ACTIVE
 *   (status: ACTIVE (default), UNASSIGNED, a DeliveryStatus name, or ALL)
 *
 * A rider (DELIVERY_STAFF) sees only the deliveries given to them.
 * The admin sees all of them and can assign riders.
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
        String filter = TextUtil.clean(request.getParameter("status")).toUpperCase();
        boolean known = DeliveryStatus.fromText(filter) != null
                || DeliveryDAO.FILTER_ALL.equals(filter)
                || (DeliveryDAO.FILTER_UNASSIGNED.equals(filter) && user.isAdmin());
        if (!known) {
            filter = DeliveryDAO.FILTER_ACTIVE;
        }

        try {
            request.setAttribute("deliveries", deliveryService.getDeliveries(user, filter));
            request.setAttribute("counts", deliveryService.getCounts(user));
            if (user.isAdmin()) {
                request.setAttribute("riders", deliveryService.getRiders());
            }
        } catch (SQLException e) {
            throw new ServletException("Could not load the deliveries", e);
        }
        request.setAttribute("filter", filter);
        request.getRequestDispatcher("/WEB-INF/views/delivery/manage-deliveries.jsp").forward(request, response);
    }
}
