package com.medisys.servlet.medicine;

import com.medisys.model.Category;
import com.medisys.service.MedicineService;
import com.medisys.service.ValidationException;
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
 * Admin page to list, add and delete medicine categories.
 *
 *   GET  /admin/categories
 *   POST /admin/categories   action=add&name=...&description=...
 *   POST /admin/categories   action=delete&id=3
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet("/admin/categories")
public class CategoryServlet extends HttpServlet {

    private final MedicineService medicineService = new MedicineService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("categories", medicineService.getCategories());
        } catch (SQLException e) {
            throw new ServletException("Could not load the categories", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/medicine/categories.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = TextUtil.clean(request.getParameter("action"));
        try {
            if ("add".equals(action)) {
                String name = request.getParameter("name");
                medicineService.addCategory(name, request.getParameter("description"));
                SessionUtil.flash(request, "success", "Category \"" + TextUtil.clean(name) + "\" was added.");
            } else if ("delete".equals(action)) {
                Integer id = TextUtil.parseInt(request.getParameter("id"));
                if (id == null) {
                    throw new ValidationException("No category was selected.");
                }
                Category deleted = medicineService.deleteCategory(id);
                SessionUtil.flash(request, "success", "Category \"" + deleted.getName() + "\" was deleted.");
            }
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", String.join(" ", e.getErrors()));
        } catch (SQLException e) {
            throw new ServletException("Could not update the categories", e);
        }
        response.sendRedirect(request.getContextPath() + "/admin/categories");
    }
}
