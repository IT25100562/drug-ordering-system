package com.medisys.servlet.cart;

import com.medisys.model.User;
import com.medisys.model.WishlistItem;
import com.medisys.service.CartService;
import com.medisys.service.WishlistService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shows the customer's wishlist.
 *
 *   GET /wishlist
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
@WebServlet("/wishlist")
public class WishlistServlet extends HttpServlet {

    private final WishlistService wishlistService = new WishlistService();
    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = CartReply.customer(request);
        try {
            List<WishlistItem> items = wishlistService.getWishlist(user.getId());

            // Which saved medicines still need a prescription upload before buying.
            Set<Integer> needsPrescription = new HashSet<>();
            for (WishlistItem item : items) {
                if (cartService.needsPrescriptionUpload(user.getId(), item.getMedicine())) {
                    needsPrescription.add(item.getMedicine().getId());
                }
            }
            request.setAttribute("items", items);
            request.setAttribute("needsPrescription", needsPrescription);
            request.setAttribute("cartQuantities", cartService.getQuantities(user.getId()));
        } catch (SQLException e) {
            throw new ServletException("Could not load the wishlist", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/cart/wishlist.jsp").forward(request, response);
    }
}
