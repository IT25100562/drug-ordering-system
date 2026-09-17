package com.medisys.servlet.medicine;

import com.medisys.model.Medicine;
import com.medisys.model.User;
import com.medisys.service.CartService;
import com.medisys.service.MedicineService;
import com.medisys.service.WishlistService;
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
 * Details page for one medicine.
 *
 *   GET /medicines/view?id=5
 *
 * Discontinued and expired medicines are shown as "not found" to customers.
 * For a logged in customer it also sends the cart / wishlist / prescription
 * state of this medicine (module 01 and 05).
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet("/medicines/view")
public class MedicineDetailsServlet extends HttpServlet {

    private final MedicineService medicineService = new MedicineService();
    private final CartService cartService = new CartService();
    private final WishlistService wishlistService = new WishlistService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        User user = SessionUtil.currentUser(request);

        try {
            Medicine medicine = id == null ? null : medicineService.getCatalogMedicine(id);
            if (medicine == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("medicine", medicine);
            request.setAttribute("related", medicineService.getRelated(medicine));
            request.setAttribute("maxQuantity", cartService.maxQuantity(medicine));

            int inCart = 0;
            boolean saved = false;
            // Guests and staff see the prescription notice too; only a customer can have one approved.
            boolean needsPrescription = medicine.isRequiresPrescription();
            if (user != null && user.isCustomer()) {
                inCart = cartService.getQuantity(user.getId(), medicine.getId());
                saved = wishlistService.isSaved(user.getId(), medicine.getId());
                needsPrescription = cartService.needsPrescriptionUpload(user.getId(), medicine);
            }
            request.setAttribute("inCart", inCart);
            request.setAttribute("saved", saved);
            request.setAttribute("needsPrescription", needsPrescription);
        } catch (SQLException e) {
            throw new ServletException("Could not load the medicine", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/medicine/medicine-details.jsp").forward(request, response);
    }
}
