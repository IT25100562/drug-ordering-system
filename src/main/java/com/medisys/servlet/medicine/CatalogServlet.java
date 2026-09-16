package com.medisys.servlet.medicine;

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
 * Public medicine catalog with search and category filter.
 *
 *   GET /medicines?q=pana&category=3
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet("/medicines")
public class CatalogServlet extends HttpServlet {

    private final MedicineService medicineService = new MedicineService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = TextUtil.clean(request.getParameter("q"));
        Integer categoryId = TextUtil.parseInt(request.getParameter("category"));

        try {
            request.setAttribute("medicines", medicineService.getCatalog(keyword, categoryId));
            request.setAttribute("categories", medicineService.getCategories());
        } catch (SQLException e) {
            throw new ServletException("Could not load the catalog", e);
        }
        request.setAttribute("keyword", keyword);
        request.setAttribute("selectedCategory", categoryId);
        request.getRequestDispatcher("/WEB-INF/views/medicine/catalog.jsp").forward(request, response);
    }
}
