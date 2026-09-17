package com.medisys.servlet.medicine;

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
import java.util.Collections;

/**
 * Public medicine catalog with search, category filter and sorting.
 *
 *   GET /medicines?q=pana&category=3&sort=price-asc
 *
 * For a logged in customer it also sends which medicines are already in the
 * cart and the wishlist (module 01), so the cards can show that.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet("/medicines")
public class CatalogServlet extends HttpServlet {

    private final MedicineService medicineService = new MedicineService();
    private final CartService cartService = new CartService();
    private final WishlistService wishlistService = new WishlistService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = TextUtil.clean(request.getParameter("q"));
        Integer categoryId = TextUtil.parseInt(request.getParameter("category"));
        String sort = TextUtil.clean(request.getParameter("sort"));
        User user = SessionUtil.currentUser(request);

        try {
            request.setAttribute("medicines", medicineService.getCatalog(keyword, categoryId, sort));
            request.setAttribute("categories", medicineService.getCategories());
            if (user != null && user.isCustomer()) {
                request.setAttribute("cartQuantities", cartService.getQuantities(user.getId()));
                request.setAttribute("wishlistIds", wishlistService.getMedicineIds(user.getId()));
            } else {
                request.setAttribute("cartQuantities", Collections.emptyMap());
                request.setAttribute("wishlistIds", Collections.emptySet());
            }
        } catch (SQLException e) {
            throw new ServletException("Could not load the catalog", e);
        }
        request.setAttribute("keyword", keyword);
        request.setAttribute("selectedCategory", categoryId);
        request.setAttribute("sort", sort);
        request.getRequestDispatcher("/WEB-INF/views/medicine/catalog.jsp").forward(request, response);
    }
}
