package com.medisys.servlet.cart;

import com.medisys.model.User;
import com.medisys.service.CartService;
import com.medisys.service.WishlistService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Shows the customer's cart.
 *
 *   GET /cart
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    private final CartService cartService = new CartService();
    private final WishlistService wishlistService = new WishlistService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = CartReply.customer(request);
        try {
            request.setAttribute("cart", cartService.getCart(user.getId()));
            request.setAttribute("wishlistCount", wishlistService.count(user.getId()));
        } catch (SQLException e) {
            throw new ServletException("Could not load the cart", e);
        }
        request.setAttribute("maxPerItem", CartService.MAX_QUANTITY_PER_ITEM);
        request.getRequestDispatcher("/WEB-INF/views/cart/cart.jsp").forward(request, response);
    }
}
