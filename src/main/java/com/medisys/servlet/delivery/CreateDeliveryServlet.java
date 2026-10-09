package com.medisys.servlet.delivery;

import com.medisys.dao.DeliveryDAO;
import com.medisys.dao.impl.DeliveryDAOImpl;
import com.medisys.model.Delivery;
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
 * Creates a delivery manually.
 * Admin only.
 */
@WebServlet("/staff/deliveries/create")
public class CreateDeliveryServlet extends HttpServlet {

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
            int orderId = Integer.parseInt(request.getParameter("orderId"));
            String staffIdStr = request.getParameter("staffId");
            
            Delivery delivery = new Delivery();
            delivery.setOrderId(orderId);
            if (staffIdStr != null && !staffIdStr.trim().isEmpty()) {
                delivery.setStaffId(Integer.parseInt(staffIdStr));
            }
            
            deliveryDAO.create(delivery);
            SessionUtil.flash(request, "success", "Delivery record created successfully.");
        } catch (NumberFormatException e) {
            SessionUtil.flash(request, "error", "Invalid inputs for creating delivery.");
        } catch (SQLException e) {
            throw new ServletException("Could not create delivery", e);
        }

        response.sendRedirect(request.getContextPath() + "/staff/deliveries");
    }
}
