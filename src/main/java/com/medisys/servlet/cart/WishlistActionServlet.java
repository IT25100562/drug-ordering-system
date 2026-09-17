package com.medisys.servlet.cart;

import com.medisys.model.Medicine;
import com.medisys.service.PrescriptionRequiredException;
import com.medisys.service.ValidationException;
import com.medisys.service.WishlistService;
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
 * Everything you can do with a wishlist item.
 *
 *   POST /wishlist/action   medicineId=5&action=toggle          heart button
 *   POST /wishlist/action   medicineId=5&action=add
 *   POST /wishlist/action   medicineId=5&action=remove
 *   POST /wishlist/action   medicineId=5&action=move            wishlist -> cart
 *   POST /wishlist/action   medicineId=5&action=save-for-later  cart -> wishlist
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
@WebServlet("/wishlist/action")
public class WishlistActionServlet extends HttpServlet {

    private final WishlistService wishlistService = new WishlistService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = CartReply.customer(request).getId();
        Integer medicineId = TextUtil.parseInt(request.getParameter("medicineId"));
        String action = TextUtil.clean(request.getParameter("action"));
        Map<String, Object> extra = new LinkedHashMap<>();

        try {
            try {
                if (medicineId == null) {
                    throw new ValidationException("No medicine was selected.");
                }
                String message;
                String defaultPath = "/wishlist";

                switch (action) {
                    case "toggle": {
                        boolean saved = wishlistService.toggle(userId, medicineId);
                        extra.put("saved", saved);
                        message = saved ? "Saved to your wishlist." : "Removed from your wishlist.";
                        defaultPath = "/medicines";
                        break;
                    }
                    case "add": {
                        Medicine m = wishlistService.add(userId, medicineId);
                        extra.put("saved", true);
                        message = m.getDisplayName() + " was saved to your wishlist.";
                        defaultPath = "/medicines/view?id=" + medicineId;
                        break;
                    }
                    case "remove": {
                        Medicine m = wishlistService.remove(userId, medicineId);
                        extra.put("saved", false);
                        message = (m == null ? "The item" : m.getDisplayName()) + " was removed from your wishlist.";
                        break;
                    }
                    case "move": {
                        message = wishlistService.moveToCart(userId, medicineId);
                        extra.put("saved", false);
                        break;
                    }
                    case "save-for-later": {
                        Medicine m = wishlistService.saveForLater(userId, medicineId);
                        extra.putAll(CartReply.cartState(userId, medicineId));
                        message = m.getDisplayName() + " was moved to your wishlist.";
                        defaultPath = "/cart";
                        break;
                    }
                    default:
                        throw new ValidationException("Unknown wishlist action.");
                }
                CartReply.send(request, response, true, message, extra, defaultPath);

            } catch (PrescriptionRequiredException e) {
                // "Move to cart" on a prescription-only medicine.
                String uploadPath = "/prescriptions/upload?medicineId=" + e.getMedicine().getId();
                if (JsonUtil.wantsJson(request)) {
                    extra.put("prescriptionRequired", true);
                    extra.put("uploadUrl", request.getContextPath() + uploadPath);
                    CartReply.send(request, response, false, e.getMessage(), extra, "/wishlist");
                } else {
                    SessionUtil.flash(request, "info", e.getMessage());
                    response.sendRedirect(request.getContextPath() + uploadPath);
                }
            } catch (ValidationException e) {
                CartReply.send(request, response, false, e.getMessage(), extra, "/wishlist");
            }
        } catch (SQLException e) {
            throw new ServletException("Could not update the wishlist", e);
        }
    }
}
