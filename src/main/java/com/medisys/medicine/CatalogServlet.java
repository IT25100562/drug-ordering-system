package com.medisys.medicine;

import com.medisys.cart.Cart;
import com.medisys.cart.CartDAO;
import com.medisys.cart.WishlistDAO;
import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.user.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The catalog customers browse (READ only). Guests can use it too.
 *
 *   GET /medicines?q=pan&category=2&sort=price-asc   search, filter and sort
 *   GET /medicines/view?id=5                         one medicine, with related ones
 *
 * Only medicines that are not discontinued and not expired are shown.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet({"/medicines", "/medicines/view"})
public class CatalogServlet extends HttpServlet {

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final WishlistDAO wishlistDAO = new WishlistDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/medicines/view")) {
                showMedicine(request, response);
            } else {
                showCatalog(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not load the catalog", e);
        }
    }

    /** The list of medicines. */
    private void showCatalog(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        String keyword = TextUtil.clean(request.getParameter("q"));
        Integer categoryId = TextUtil.parseInt(request.getParameter("category"));
        String sort = TextUtil.clean(request.getParameter("sort"));
        User user = SessionUtil.currentUser(request);

        List<Medicine> medicines = medicineDAO.searchMedicines(keyword, categoryId, MedicineDAO.FILTER_CATALOG);
        if ("price-asc".equals(sort)) {
            medicines.sort(Comparator.comparing(Medicine::getPrice));
        } else if ("price-desc".equals(sort)) {
            medicines.sort(Comparator.comparing(Medicine::getPrice).reversed());
        }

        request.setAttribute("medicines", medicines);
        request.setAttribute("categories", categoryDAO.getAllCategories());
        if (user != null && user.isCustomer()) {
            request.setAttribute("cartQuantities", cartDAO.getQuantities(user.getId()));
            request.setAttribute("wishlistIds", wishlistDAO.getMedicineIds(user.getId()));
        } else {
            request.setAttribute("cartQuantities", Collections.emptyMap());
            request.setAttribute("wishlistIds", Collections.emptySet());
        }
        request.setAttribute("keyword", keyword);
        request.setAttribute("selectedCategory", categoryId);
        request.setAttribute("sort", sort);
        request.getRequestDispatcher("/WEB-INF/views/medicine/catalog.jsp").forward(request, response);
    }

    /** One medicine. */
    private void showMedicine(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Medicine medicine = id == null ? null : medicineDAO.getCatalogMedicine(id);
        if (medicine == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Up to 4 other medicines from the same category, in stock ones first.
        List<Medicine> related = new ArrayList<>();
        for (Medicine m : medicineDAO.searchMedicines(null, medicine.getCategoryId(), MedicineDAO.FILTER_CATALOG)) {
            if (m.getId() != medicine.getId()) {
                related.add(m);
            }
        }
        related.sort(Comparator.comparing(Medicine::isOutOfStock));
        request.setAttribute("related", related.size() > 4 ? related.subList(0, 4) : related);

        // Guests and staff see the prescription notice too; only a customer can buy.
        User user = SessionUtil.currentUser(request);
        int maxQuantity = Cart.maxQuantity(medicine);
        int inCart = 0;
        boolean saved = false;
        if (user != null && user.isCustomer()) {
            inCart = cartDAO.getQuantity(user.getId(), medicine.getId());
            saved = wishlistDAO.isSaved(user.getId(), medicine.getId());
            if (medicine.isRequiresPrescription()) {
                maxQuantity = 0;
            }
        }
        request.setAttribute("medicine", medicine);
        request.setAttribute("maxQuantity", maxQuantity);
        request.setAttribute("inCart", inCart);
        request.setAttribute("saved", saved);
        request.setAttribute("needsPrescription", medicine.isRequiresPrescription());
        request.getRequestDispatcher("/WEB-INF/views/medicine/medicine-details.jsp").forward(request, response);
    }
}
