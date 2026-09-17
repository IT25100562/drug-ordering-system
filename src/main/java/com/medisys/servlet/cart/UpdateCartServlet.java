package com.medisys.servlet.cart;

import com.medisys.service.CartService;
import com.medisys.service.ValidationException;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Changes the quantity of a cart line (0 removes it).
 *
 *   POST /cart/update   medicineId=5&quantity=3
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
@WebServlet("/cart/update")
public class UpdateCartServlet extends HttpServlet {

    private final CartService cartService = new CartService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = CartReply.customer(request).getId();
        Integer medicineId = TextUtil.parseInt(request.getParameter("medicineId"));

        try {
            boolean ok;
            String message;
            try {
                if (medicineId == null) {
                    throw new ValidationException("No medicine was selected.");
                }
                message = cartService.updateQuantity(userId, medicineId, request.getParameter("quantity"));
                ok = true;
            } catch (ValidationException e) {
                message = e.getMessage();
                ok = false;
            }
            // Always send the real state back, so the page can undo a refused change.
            CartReply.send(request, response, ok, message,
                    medicineId == null ? null : CartReply.cartState(userId, medicineId), "/cart");
        } catch (SQLException e) {
            throw new ServletException("Could not update the cart", e);
        }
    }
}
