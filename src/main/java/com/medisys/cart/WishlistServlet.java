package com.medisys.cart;

import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.ValidationException;
import com.medisys.medicine.Medicine;
import com.medisys.medicine.MedicineDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The wishlist ("save for later").
 *
 *   GET  /wishlist                                    the saved medicines          READ
 *   POST /wishlist/action  action=add                 save a medicine              CREATE
 *   POST /wishlist/action  action=toggle              the heart button: save / unsave
 *   POST /wishlist/action  action=remove              remove a saved medicine      DELETE
 *   POST /wishlist/action  action=move                into the cart, out of the wishlist
 *   POST /wishlist/action  action=save-for-later      out of the cart, into the wishlist
 *
 * Any medicine in the catalog can be saved, even when it is out of stock (that
 * is the point: "tell me later"). "Move to cart" follows all the cart rules.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
@WebServlet({"/wishlist", "/wishlist/action"})
public class WishlistServlet extends HttpServlet {

    private final WishlistDAO wishlistDAO = new WishlistDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();

    // ================================================================== READ

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = SessionUtil.currentUser(request).getId();
        try {
            List<WishlistItem> items = wishlistDAO.getWishlist(userId);
            // Which saved medicines need a prescription upload before buying.
            Set<Integer> needsPrescription = new HashSet<>();
            for (WishlistItem item : items) {
                if (item.getMedicine().isRequiresPrescription()) {
                    needsPrescription.add(item.getMedicine().getId());
                }
            }
            request.setAttribute("items", items);
            request.setAttribute("needsPrescription", needsPrescription);
            request.setAttribute("cartQuantities", cartDAO.getQuantities(userId));
        } catch (SQLException e) {
            throw new ServletException("Could not load the wishlist", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/cart/wishlist.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = SessionUtil.currentUser(request).getId();
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
                        boolean saved = !wishlistDAO.isSaved(userId, medicineId);
                        if (saved) {
                            addToWishlist(userId, medicineId);                      // CREATE
                        } else {
                            wishlistDAO.removeItem(userId, medicineId);             // DELETE
                        }
                        extra.put("saved", saved);
                        message = saved ? "Saved to your wishlist." : "Removed from your wishlist.";
                        defaultPath = "/medicines";
                        break;
                    }
                    case "add": {
                        Medicine m = addToWishlist(userId, medicineId);             // CREATE
                        extra.put("saved", true);
                        message = m.getDisplayName() + " was saved to your wishlist.";
                        defaultPath = "/medicines/view?id=" + medicineId;
                        break;
                    }
                    case "remove": {
                        if (!wishlistDAO.removeItem(userId, medicineId)) {          // DELETE
                            throw new ValidationException("That item is not in your wishlist.");
                        }
                        Medicine m = medicineDAO.getMedicineById(medicineId);
                        extra.put("saved", false);
                        message = (m == null ? "The item" : m.getDisplayName()) + " was removed from your wishlist.";
                        break;
                    }
                    case "move": {
                        // One pack into the cart (throws if the cart rules say no), then out of the wishlist.
                        message = CartServlet.addToCart(userId, medicineId, "1");
                        wishlistDAO.removeItem(userId, medicineId);
                        extra.put("saved", false);
                        break;
                    }
                    case "save-for-later": {
                        Medicine m = CartServlet.findLine(userId, medicineId);
                        cartDAO.removeItem(userId, medicineId);
                        wishlistDAO.addItem(userId, medicineId);
                        extra.putAll(CartServlet.cartState(userId, medicineId));
                        message = m.getDisplayName() + " was moved to your wishlist.";
                        defaultPath = "/cart";
                        break;
                    }
                    default:
                        throw new ValidationException("Unknown wishlist action.");
                }
                CartServlet.reply(request, response, true, message, extra, defaultPath);

            } catch (CartServlet.PrescriptionNeededException e) {
                // "Move to cart" on a prescription-only medicine.
                CartServlet.sendToPrescriptionUpload(request, response, e, extra, "/wishlist");
            } catch (ValidationException e) {
                CartServlet.reply(request, response, false, e.getMessage(), extra, "/wishlist");
            }
        } catch (SQLException e) {
            throw new ServletException("Could not update the wishlist", e);
        }
    }

    /** Saves a medicine that is in the catalog. */
    private Medicine addToWishlist(int userId, int medicineId) throws SQLException, ValidationException {
        Medicine medicine = medicineDAO.getCatalogMedicine(medicineId);
        if (medicine == null) {
            throw new ValidationException("This medicine is not available.");
        }
        wishlistDAO.addItem(userId, medicineId);
        return medicine;
    }
}
