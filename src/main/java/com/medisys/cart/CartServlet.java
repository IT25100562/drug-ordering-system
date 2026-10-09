package com.medisys.cart;

import com.medisys.common.JsonUtil;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The shopping cart: all its CRUD.
 *
 *   GET  /cart                                   the cart page                READ
 *   POST /cart/add      medicineId, quantity     add (or raise the quantity)  CREATE
 *   POST /cart/update   medicineId, quantity     change the quantity (0 = remove)  UPDATE
 *   POST /cart/remove   medicineId               remove one line              DELETE
 *   POST /cart/remove   all=true                 empty the cart               DELETE
 *
 * Rules:
 *  - only medicines on sale (not discontinued / expired / out of stock) can be added
 *  - a prescription-only medicine never goes into the cart: the customer uploads
 *    the prescription (module 05) and pays for it there
 *  - the quantity of one medicine is 1 .. min(stock, 10)
 *
 * The page's JavaScript (cart.js) sends "Accept: application/json" and gets a
 * small JSON answer, so the page updates without reloading. Without
 * JavaScript the form posts normally and the servlet redirects back.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
@WebServlet({"/cart", "/cart/add", "/cart/update", "/cart/remove"})
public class CartServlet extends HttpServlet {

    /** SQL Server error numbers for "duplicate key". */
    private static final int DUPLICATE_KEY = 2627;
    private static final int DUPLICATE_INDEX = 2601;

    private static final CartDAO cartDAO = new CartDAO();
    private static final WishlistDAO wishlistDAO = new WishlistDAO();
    private static final MedicineDAO medicineDAO = new MedicineDAO();

    /** Thrown when a prescription-only medicine is added: the customer must upload a prescription. */
    public static class PrescriptionNeededException extends ValidationException {
        public PrescriptionNeededException(Medicine medicine) {
            super(medicine.getDisplayName() + " needs a prescription. Upload your prescription and our "
                    + "pharmacist will list the medicines for you to pay.");
        }
    }

    // ================================================================== READ

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = SessionUtil.currentUser(request).getId();
        try {
            request.setAttribute("cart", cartDAO.getCart(userId));
            request.setAttribute("wishlistCount", wishlistDAO.countItems(userId));
        } catch (SQLException e) {
            throw new ServletException("Could not load the cart", e);
        }
        request.setAttribute("maxPerItem", Cart.MAX_PER_ITEM);
        request.getRequestDispatcher("/WEB-INF/views/cart/cart.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int userId = SessionUtil.currentUser(request).getId();
        Integer medicineId = TextUtil.parseInt(request.getParameter("medicineId"));
        Map<String, Object> extra = new LinkedHashMap<>();
        try {
            try {
                String message;
                switch (request.getServletPath()) {
                    case "/cart/add":
                        message = addToCart(userId, medicineId, request.getParameter("quantity"));
                        extra.put("inCart", cartDAO.getQuantity(userId, medicineId));
                        break;
                    case "/cart/update":
                        try {
                            message = updateQuantity(userId, medicineId, request.getParameter("quantity"));
                        } finally {
                            // Always send the real state back, so the page can undo a refused change.
                            if (medicineId != null) {
                                extra.putAll(cartState(userId, medicineId));
                            }
                        }
                        break;
                    default:
                        if ("true".equals(request.getParameter("all"))) {
                            cartDAO.clearCart(userId);
                            reply(request, response, true, "Your cart is now empty.", null, "/cart");
                            return;
                        }
                        message = removeItem(userId, medicineId);
                        extra.putAll(cartState(userId, medicineId));
                }
                reply(request, response, true, message, extra, "/cart");

            } catch (PrescriptionNeededException e) {
                sendToPrescriptionUpload(request, response, e, extra, "/cart");
            } catch (ValidationException e) {
                reply(request, response, false, e.getMessage(), extra, "/cart");
            }
        } catch (SQLException e) {
            throw new ServletException("Could not update the cart", e);
        }
    }

    // ================================================================ CREATE

    /**
     * Adds a medicine to the cart, or raises its quantity. The wishlist ("move
     * to cart") and "order again" use this too, so the rules are the same everywhere.
     *
     * @return a message for the customer, e.g. "Panadol 500 mg was added to your cart."
     */
    public static String addToCart(int userId, Integer medicineId, String quantityText)
            throws SQLException, ValidationException {
        // ---- validation
        Integer quantity = TextUtil.parseInt(quantityText);
        if (medicineId == null) {
            throw new ValidationException("No medicine was selected.");
        }
        if (quantity == null || quantity < 1) {
            throw new ValidationException("Please choose a quantity of at least 1.");
        }
        Medicine medicine = medicineDAO.getCatalogMedicine(medicineId);
        if (medicine == null) {
            throw new ValidationException("This medicine is not available.");
        }
        if (medicine.isOutOfStock()) {
            throw new ValidationException(medicine.getDisplayName() + " is out of stock.");
        }
        if (medicine.isRequiresPrescription()) {
            throw new PrescriptionNeededException(medicine);
        }
        int limit = Cart.maxQuantity(medicine);
        int inCart = cartDAO.getQuantity(userId, medicineId);
        if (inCart >= limit) {
            throw new ValidationException("You already have the most you can buy of "
                    + medicine.getDisplayName() + " (" + limit + ") in your cart.");
        }

        int newQuantity = Math.min(inCart + quantity, limit);
        if (inCart == 0) {
            try {
                cartDAO.addItem(userId, medicineId, newQuantity);
            } catch (SQLException e) {
                // Two clicks at the same moment: the line was just created, so update it.
                if (e.getErrorCode() != DUPLICATE_KEY && e.getErrorCode() != DUPLICATE_INDEX) {
                    throw e;
                }
                cartDAO.updateQuantity(userId, medicineId, newQuantity);
            }
        } else {
            cartDAO.updateQuantity(userId, medicineId, newQuantity);
        }

        String name = medicine.getDisplayName();
        if (newQuantity < inCart + quantity) {
            return "Only " + limit + " of " + name + " can be bought, so your cart now has " + limit + ".";
        }
        if (inCart > 0) {
            return "You now have " + newQuantity + " of " + name + " in your cart.";
        }
        return name + " was added to your cart.";
    }

    // ================================================================ UPDATE

    /** Sets the quantity of a line. 0 removes the line. */
    private String updateQuantity(int userId, Integer medicineId, String quantityText)
            throws SQLException, ValidationException {
        // ---- validation
        if (medicineId == null) {
            throw new ValidationException("No medicine was selected.");
        }
        Integer quantity = TextUtil.parseInt(quantityText);
        if (quantity == null || quantity < 0) {
            throw new ValidationException("Quantity must be a whole number.");
        }
        Medicine medicine = findLine(userId, medicineId);
        if (quantity == 0) {
            cartDAO.removeItem(userId, medicineId);
            return medicine.getDisplayName() + " was removed from your cart.";
        }
        if (quantity > Cart.MAX_PER_ITEM) {
            throw new ValidationException("You can buy at most " + Cart.MAX_PER_ITEM + " of "
                    + medicine.getDisplayName() + ".");
        }
        if (quantity > medicine.getStockQuantity()) {
            throw new ValidationException(medicine.getStockQuantity() == 0
                    ? medicine.getDisplayName() + " is out of stock."
                    : "Only " + medicine.getStockQuantity() + " of " + medicine.getDisplayName() + " left in stock.");
        }
        if (medicine.isRequiresPrescription()) {
            throw new ValidationException(medicine.getDisplayName() + " can only be ordered with a prescription.");
        }
        cartDAO.updateQuantity(userId, medicineId, quantity);
        return "Quantity of " + medicine.getDisplayName() + " changed to " + quantity + ".";
    }

    // ================================================================ DELETE

    private String removeItem(int userId, Integer medicineId) throws SQLException, ValidationException {
        Medicine medicine = findLine(userId, medicineId);
        cartDAO.removeItem(userId, medicineId);
        return medicine.getDisplayName() + " was removed from your cart.";
    }

    // =============================================================== helpers

    /** The medicine of a cart line, or an error when the line is not in this customer's cart. */
    static Medicine findLine(int userId, Integer medicineId) throws SQLException, ValidationException {
        if (medicineId == null) {
            throw new ValidationException("No medicine was selected.");
        }
        Medicine medicine = cartDAO.getQuantity(userId, medicineId) == 0 ? null : medicineDAO.getMedicineById(medicineId);
        if (medicine == null) {
            throw new ValidationException("That item is no longer in your cart.");
        }
        return medicine;
    }

    /**
     * Sends the answer: a small JSON object for our JavaScript, otherwise a
     * flash message and a redirect back to the page the form was on.
     *
     * @param extra       more JSON values (may be null)
     * @param defaultPath where a plain form goes when it has no returnTo
     */
    static void reply(HttpServletRequest request, HttpServletResponse response, boolean ok,
                      String message, Map<String, Object> extra, String defaultPath)
            throws IOException, SQLException {
        if (JsonUtil.wantsJson(request)) {
            int userId = SessionUtil.currentUser(request).getId();
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("ok", ok);
            json.put("message", message);
            json.put("cartCount", cartDAO.countItems(userId));
            json.put("wishlistCount", wishlistDAO.countItems(userId));
            if (extra != null) {
                json.putAll(extra);
            }
            JsonUtil.write(response, ok ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST, json);
            return;
        }
        SessionUtil.flash(request, ok ? "success" : "error", message);
        String back = request.getParameter("returnTo");
        response.sendRedirect(request.getContextPath() + (TextUtil.isSafeLocalPath(back) ? back : defaultPath));
    }

    /** A prescription-only medicine: send the customer to the prescription upload page. */
    static void sendToPrescriptionUpload(HttpServletRequest request, HttpServletResponse response,
                                         ValidationException e, Map<String, Object> extra, String defaultPath)
            throws IOException, SQLException {
        String uploadPath = "/prescriptions/upload";
        if (JsonUtil.wantsJson(request)) {
            extra.put("prescriptionRequired", true);
            extra.put("uploadUrl", request.getContextPath() + uploadPath);
            reply(request, response, false, e.getMessage(), extra, defaultPath);
        } else {
            SessionUtil.flash(request, "info", e.getMessage());
            response.sendRedirect(request.getContextPath() + uploadPath);
        }
    }

    /**
     * The numbers the cart page needs after a change: the subtotal, and the
     * line's quantity, total and problem (if the line still exists).
     */
    static Map<String, Object> cartState(int userId, int medicineId) throws SQLException {
        Cart cart = cartDAO.getCart(userId);
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
