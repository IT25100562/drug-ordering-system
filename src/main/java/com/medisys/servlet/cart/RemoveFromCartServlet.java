package com.medisys.servlet.cart;

import com.medisys.model.Medicine;
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
 * Removes one line from the cart, or empties the whole cart.
 *
 *   POST /cart/remove   medicineId=5
 *   POST /cart/remove   all=true
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
@WebServlet("/cart/remove")
public class RemoveFromCartServlet extends HttpServlet {

    private final CartService cartService = new CartService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = CartReply.customer(request).getId();

        try {
            if ("true".equals(request.getParameter("all"))) {
                cartService.clearCart(userId);
                CartReply.send(request, response, true, "Your cart is now empty.", null, "/cart");
                return;
            }

            Integer medicineId = TextUtil.parseInt(request.getParameter("medicineId"));
            try {
                if (medicineId == null) {
                    throw new ValidationException("No medicine was selected.");
                }
                Medicine removed = cartService.removeItem(userId, medicineId);
                CartReply.send(request, response, true,
                        removed.getDisplayName() + " was removed from your cart.",
                        CartReply.cartState(userId, medicineId), "/cart");
            } catch (ValidationException e) {
                CartReply.send(request, response, false, e.getMessage(), null, "/cart");
            }
        } catch (SQLException e) {
            throw new ServletException("Could not update the cart", e);
        }
    }
}
