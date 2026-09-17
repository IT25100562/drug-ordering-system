package com.medisys.servlet.order;

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
 * "Buy again": puts the medicines of an old order back into the cart.
 *
 *   POST /orders/reorder   id=12
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet("/orders/reorder")
public class ReorderServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            if (id == null) {
                throw new ValidationException("That order was not found.");
            }
            String message = orderService.reorder(SessionUtil.currentUser(request), id);
            SessionUtil.flash(request, "success", message);
            response.sendRedirect(request.getContextPath() + "/cart");
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
            response.sendRedirect(request.getContextPath() + (id == null ? "/orders" : "/orders/view?id=" + id));
        } catch (SQLException e) {
            throw new ServletException("Could not add the order to the cart", e);
        }
    }
}
