package com.medisys.servlet.delivery;

import com.medisys.dao.DeliveryDAO;
import com.medisys.dao.impl.DeliveryDAOImpl;
import com.medisys.model.User;
import com.medisys.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Soft deletes a delivery manually.
 * Admin only.
 */
@WebServlet("/staff/deliveries/delete")
public class DeleteDeliveryServlet extends HttpServlet {

    private final DeliveryDAO deliveryDAO = new DeliveryDAOImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        if (user == null || !user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            int deliveryId = Integer.parseInt(request.getParameter("id"));
            if (deliveryDAO.delete(deliveryId)) {
                SessionUtil.flash(request, "success", "Delivery record deleted successfully.");
            } else {
                SessionUtil.flash(request, "error", "Failed to delete delivery record.");
            }
        } catch (NumberFormatException e) {
            SessionUtil.flash(request, "error", "Invalid delivery ID.");
        } catch (SQLException e) {
            throw new ServletException("Could not delete delivery", e);
        }

        response.sendRedirect(request.getContextPath() + "/staff/deliveries");
    }
}
