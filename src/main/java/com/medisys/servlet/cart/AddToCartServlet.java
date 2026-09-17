package com.medisys.servlet.cart;

import com.medisys.service.CartService;
import com.medisys.service.PrescriptionRequiredException;
import com.medisys.service.ValidationException;
import com.medisys.util.JsonUtil;
import com.medisys.util.SessionUtil;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds a medicine to the cart.
 *
 *   POST /cart/add   medicineId=5&quantity=2&returnTo=/medicines
 *
 * A prescription-only medicine without an approved prescription sends the
 * customer to the prescription upload page (module 05) instead.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
@WebServlet("/cart/add")
public class AddToCartServlet extends HttpServlet {

    private final CartService cartService = new CartService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = CartReply.customer(request).getId();
        Integer medicineId = TextUtil.parseInt(request.getParameter("medicineId"));
        Map<String, Object> extra = new LinkedHashMap<>();

        try {
            try {
                if (medicineId == null) {
                    throw new ValidationException("No medicine was selected.");
                }
                String message = cartService.addToCart(userId, medicineId, request.getParameter("quantity"));
                extra.put("inCart", cartService.getQuantity(userId, medicineId));
                CartReply.send(request, response, true, message, extra, "/cart");

            } catch (PrescriptionRequiredException e) {
                String uploadPath = "/prescriptions/upload";
                if (JsonUtil.wantsJson(request)) {
                    extra.put("prescriptionRequired", true);
                    extra.put("uploadUrl", request.getContextPath() + uploadPath);
                    CartReply.send(request, response, false, e.getMessage(), extra, "/cart");
                } else {
                    SessionUtil.flash(request, "info", e.getMessage());
                    response.sendRedirect(request.getContextPath() + uploadPath);
                }
            } catch (ValidationException e) {
                CartReply.send(request, response, false, e.getMessage(), extra, "/cart");
            }
        } catch (SQLException e) {
            throw new ServletException("Could not add to the cart", e);
        }
    }
}
