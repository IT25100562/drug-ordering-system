package com.medisys.medicine;

import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The admin's medicine categories.
 *
 *   GET  /admin/categories                  list with medicine counts       READ
 *   POST /admin/categories  action=add      add a category                  CREATE
 *   POST /admin/categories  action=update   rename / change the description UPDATE
 *   POST /admin/categories  action=delete   delete an unused category       DELETE
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet("/admin/categories")
public class CategoryServlet extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("categories", categoryDAO.getAllCategories());       // READ
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
                addCategory(request);
            } else if ("update".equals(action)) {
                updateCategory(request);
            } else if ("delete".equals(action)) {
                deleteCategory(request);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not update the categories", e);
        }
        response.sendRedirect(request.getContextPath() + "/admin/categories");
    }

    // ================================================================ CREATE

    private void addCategory(HttpServletRequest request) throws SQLException {
        String name = TextUtil.clean(request.getParameter("name"));
        String description = TextUtil.clean(request.getParameter("description"));
        if (!isValid(request, name, description, 0)) {
            return;
        }
        categoryDAO.addCategory(new Category(0, name, description.isEmpty() ? null : description));
        SessionUtil.flash(request, "success", "Category \"" + name + "\" was added.");
    }

    // ================================================================ UPDATE

    private void updateCategory(HttpServletRequest request) throws SQLException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Category category = id == null ? null : categoryDAO.getCategoryById(id);
        if (category == null) {
            SessionUtil.flash(request, "error", "That category no longer exists.");
            return;
        }
        String name = TextUtil.clean(request.getParameter("name"));
        String description = TextUtil.clean(request.getParameter("description"));
        if (!isValid(request, name, description, id)) {
            return;
        }
        categoryDAO.updateCategory(new Category(id, name, description.isEmpty() ? null : description));
        SessionUtil.flash(request, "success", "Category \"" + name + "\" was saved.");
    }

    /** VALIDATION shared by add and update. Shows the problems and returns false when there are any. */
    private boolean isValid(HttpServletRequest request, String name, String description, int id)
            throws SQLException {
        List<String> errors = new ArrayList<>();
        if (name.length() < 2 || name.length() > 100) {
            errors.add("Category name must have 2 to 100 characters.");
        } else if (categoryDAO.categoryExists(name, id)) {
            errors.add("A category called \"" + name + "\" already exists.");
        }
        if (description.length() > 255) {
            errors.add("Description can have at most 255 characters.");
        }
        if (!errors.isEmpty()) {
            SessionUtil.flash(request, "error", String.join(" ", errors));
            return false;
        }
        return true;
    }

    // ================================================================ DELETE

    /** Deletes a category, but only when no medicine uses it. */
    private void deleteCategory(HttpServletRequest request) throws SQLException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Category category = id == null ? null : categoryDAO.getCategoryById(id);
        if (category == null) {
            SessionUtil.flash(request, "error", "That category no longer exists.");
            return;
        }
        int used = categoryDAO.countMedicines(id);
        if (used > 0) {
            SessionUtil.flash(request, "error", "\"" + category.getName() + "\" is used by " + used
                    + " medicine(s). Move them to another category first.");
            return;
        }
        categoryDAO.deleteCategory(id);
        SessionUtil.flash(request, "success", "Category \"" + category.getName() + "\" was deleted.");
    }
}
