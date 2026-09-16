package com.medisys.servlet.medicine;

import com.medisys.dao.MedicineDAO;
import com.medisys.service.MedicineService;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Admin inventory list with stock levels, search and filters.
 *
 *   GET /admin/medicines?q=&category=&filter=LOW_STOCK
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet("/admin/medicines")
public class InventoryServlet extends HttpServlet {

    private final MedicineService medicineService = new MedicineService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = TextUtil.clean(request.getParameter("q"));
        Integer categoryId = TextUtil.parseInt(request.getParameter("category"));
        String filter = TextUtil.clean(request.getParameter("filter"));
        if (filter.isEmpty()) {
            filter = MedicineDAO.FILTER_ACTIVE;
        }

        try {
            request.setAttribute("medicines", medicineService.getInventory(keyword, categoryId, filter));
            request.setAttribute("categories", medicineService.getCategories());
            request.setAttribute("summary", medicineService.getSummary());
        } catch (SQLException e) {
            throw new ServletException("Could not load the inventory", e);
        }
        request.setAttribute("keyword", keyword);
        request.setAttribute("selectedCategory", categoryId);
        request.setAttribute("filter", filter);
        request.getRequestDispatcher("/WEB-INF/views/medicine/inventory.jsp").forward(request, response);
    }

    /**
     * After an action on the inventory page, go back to the same list (same
     * search and filter). The query string comes from a hidden form field, so
     * only plain query characters are accepted.
     */
    static void redirectToInventory(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String query = TextUtil.clean(request.getParameter("returnQuery"));
        String url = request.getContextPath() + "/admin/medicines";
        if (query.matches("[A-Za-z0-9_=&%+.\\-]{1,300}")) {
            url += "?" + query;
        }
        response.sendRedirect(url);
    }
}
