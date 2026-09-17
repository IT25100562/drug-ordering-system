package com.medisys.servlet.cart;

import com.medisys.model.Cart;
import com.medisys.model.CartItem;
import com.medisys.model.User;
import com.medisys.service.CartService;
import com.medisys.service.WishlistService;
import com.medisys.util.JsonUtil;
import com.medisys.util.SessionUtil;
import com.medisys.util.TextUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How every cart / wishlist action answers, in one place.
 *
 *  - from our JavaScript (fetch): a small JSON object with the message and the
 *    new numbers (cart count, subtotal, ...) so the page updates in place
 *  - from a plain form (no JavaScript): a flash message and a redirect back to
 *    the page the form was on ("returnTo" field)
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
final class CartReply {

    private static final CartService CART_SERVICE = new CartService();
    private static final WishlistService WISHLIST_SERVICE = new WishlistService();

    private CartReply() {
    }

    /** The logged in customer (AuthFilter makes sure there is one). */
    static User customer(HttpServletRequest request) {
        return SessionUtil.currentUser(request);
    }

    /**
     * Sends the answer.
     *
     * @param extra       more JSON values (may be null)
     * @param defaultPath where a plain form goes when it has no returnTo
     */
    static void send(HttpServletRequest request, HttpServletResponse response, boolean ok,
                     String message, Map<String, Object> extra, String defaultPath)
            throws IOException, SQLException {
        if (JsonUtil.wantsJson(request)) {
            int userId = customer(request).getId();
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("ok", ok);
            json.put("message", message);
            json.put("cartCount", CART_SERVICE.countItems(userId));
            json.put("wishlistCount", WISHLIST_SERVICE.count(userId));
            if (extra != null) {
                json.putAll(extra);
            }
            JsonUtil.write(response, ok ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST, json);
            return;
        }

        SessionUtil.flash(request, ok ? "success" : "error", message);
        String back = request.getParameter("returnTo");
        String path = TextUtil.isSafeLocalPath(back) ? back : defaultPath;
        response.sendRedirect(request.getContextPath() + path);
    }

    /**
     * The numbers the cart page needs after a change: the subtotal, and the
     * line's quantity, total and problem (if the line still exists).
     */
    static Map<String, Object> cartState(int userId, int medicineId) throws SQLException {
        Cart cart = CART_SERVICE.getCart(userId);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("subtotal", TextUtil.money(cart.getSubtotal()));
        state.put("itemCount", cart.getItemCount());
        state.put("problemCount", cart.getProblemCount());
        state.put("readyForCheckout", cart.isReadyForCheckout());
        state.put("empty", cart.isEmpty());
        state.put("lineExists", false);
        for (CartItem item : cart.getItems()) {
            if (item.getMedicine().getId() == medicineId) {
                state.put("lineExists", true);
                state.put("quantity", item.getQuantity());
                state.put("lineTotal", TextUtil.money(item.getLineTotal()));
                state.put("problem", item.getProblem());
            }
        }
        return state;
    }
}
