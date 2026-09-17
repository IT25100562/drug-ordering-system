package com.medisys.servlet.order;

import com.medisys.model.User;
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
 * Admin moves an order to its next step, or cancels it.
 *
 *   POST /admin/orders/status   id=12&action=advance&current=PAID&note=...
 *   POST /admin/orders/status   id=12&action=cancel&reason=...
 *
 * Goes back to the page the form was on (returnTo), or the order page.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet("/admin/orders/status")
public class UpdateOrderStatusServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User admin = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        String action = TextUtil.clean(request.getParameter("action"));
        try {
            if (id == null) {
                throw new ValidationException("No order was selected.");
            }
            String message;
            if ("advance".equals(action)) {
                message = orderService.advance(admin, id, request.getParameter("current"),
                        request.getParameter("note"));
            } else if ("cancel".equals(action)) {
                message = orderService.cancelByAdmin(admin, id, request.getParameter("reason"));
            } else {
                throw new ValidationException("Unknown action.");
            }
            SessionUtil.flash(request, "success", message);
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not update the order", e);
        }

        String back = request.getParameter("returnTo");
        if (!TextUtil.isSafeLocalPath(back)) {
            back = id == null ? "/admin/orders" : "/admin/orders/view?id=" + id;
        }
        response.sendRedirect(request.getContextPath() + back);
    }
}
