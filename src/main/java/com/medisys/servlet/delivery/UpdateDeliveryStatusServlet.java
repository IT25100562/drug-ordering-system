package com.medisys.servlet.delivery;

import com.medisys.model.User;
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
 * Changes a delivery.
 *
 *   POST /staff/deliveries/update   id=3&action=status&current=DISPATCHED&next=OUT_FOR_DELIVERY&note=...
 *   POST /staff/deliveries/update   id=3&action=assign&riderId=7          (admin only)
 *
 * Goes back to the page the form was on (returnTo), or the delivery page.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
@WebServlet("/staff/deliveries/update")
public class UpdateDeliveryStatusServlet extends HttpServlet {

    private final DeliveryService deliveryService = new DeliveryService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        String action = TextUtil.clean(request.getParameter("action"));
        try {
            if (id == null) {
                throw new ValidationException("No delivery was selected.");
            }
            String message;
            if ("status".equals(action)) {
                message = deliveryService.updateStatus(user, id, request.getParameter("current"),
                        request.getParameter("next"), request.getParameter("note"));
            } else if ("assign".equals(action)) {
                message = deliveryService.assign(user, id, request.getParameter("riderId"));
            } else {
                throw new ValidationException("Unknown action.");
            }
            SessionUtil.flash(request, "success", message);
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not update the delivery", e);
        }

        String back = request.getParameter("returnTo");
        if (!TextUtil.isSafeLocalPath(back)) {
            back = id == null ? "/staff/deliveries" : "/staff/deliveries/view?id=" + id;
        }
        response.sendRedirect(request.getContextPath() + back);
    }
}
