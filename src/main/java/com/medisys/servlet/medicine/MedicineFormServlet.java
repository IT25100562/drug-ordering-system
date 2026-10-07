package com.medisys.servlet.medicine;

import com.medisys.model.Medicine;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Admin add / edit medicine form.
 *
 *   GET  /admin/medicines/edit          empty form (add)
 *   GET  /admin/medicines/edit?id=5     filled form (edit)
 *   POST /admin/medicines/edit          save, then back to the inventory
 *
 * The form values travel as a Map (field name -> text) so that, when the
 * input is wrong, the page can show exactly what the admin typed.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet("/admin/medicines/edit")
public class MedicineFormServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/medicine/medicine-form.jsp";

    /** Names of the input fields in medicine-form.jsp. */
    private static final String[] FIELDS = {
            "name", "categoryId", "manufacturer", "dosageForm", "strength", "description",
            "price", "stockQuantity", "reorderLevel", "requiresPrescription", "expiryDate"
    };

    private final MedicineService medicineService = new MedicineService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Map<String, String> form = new HashMap<>();

        try {
            if (id == null) {
                // Defaults for a new medicine.
                id = 0;
                form.put("stockQuantity", "0");
                form.put("reorderLevel", "10");
            } else {
                Medicine medicine = medicineService.getMedicine(id);
                if (medicine == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                form = toForm(medicine);
                request.setAttribute("medicine", medicine);
            }
            showForm(request, response, id, form);
        } catch (SQLException e) {
            throw new ServletException("Could not load the medicine", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer idParam = TextUtil.parseInt(request.getParameter("id"));
        int id = idParam == null ? 0 : idParam;

        Map<String, String> form = new HashMap<>();
        for (String field : FIELDS) {
            String value = request.getParameter(field);
            if (value != null) {
                form.put(field, value);
            }
        }

        try {
            int savedId = medicineService.saveMedicine(id, form);
            String name = TextUtil.clean(form.get("name"));
            SessionUtil.flash(request, "success", id == 0
                    ? "Medicine \"" + name + "\" was added (ID " + savedId + ")."
                    : "Medicine \"" + name + "\" was updated.");
            response.sendRedirect(request.getContextPath() + "/admin/medicines");
        } catch (ValidationException e) {
            // Show the same form again with the messages and the typed values.
            request.setAttribute("errors", e.getErrors());
            try {
                if (id > 0) {
                    request.setAttribute("medicine", medicineService.getMedicine(id));
                }
                showForm(request, response, id, form);
            } catch (SQLException ex) {
                throw new ServletException("Could not load the form", ex);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not save the medicine", e);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          int id, Map<String, String> form)
            throws ServletException, IOException, SQLException {
        request.setAttribute("id", id);
        request.setAttribute("form", form);
        request.setAttribute("categories", medicineService.getCategories());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** Turns a saved medicine into form values. */
    private Map<String, String> toForm(Medicine m) {
        Map<String, String> form = new HashMap<>();
        form.put("name", m.getName());
        form.put("categoryId", String.valueOf(m.getCategoryId()));
        form.put("manufacturer", m.getManufacturer());
        form.put("dosageForm", m.getDosageForm());
        form.put("strength", m.getStrength());
        form.put("description", m.getDescription());
        form.put("price", m.getPrice().toPlainString());
        form.put("stockQuantity", String.valueOf(m.getStockQuantity()));
        form.put("reorderLevel", String.valueOf(m.getReorderLevel()));
        if (m.isRequiresPrescription()) {
            form.put("requiresPrescription", "on");
        }
        if (m.getExpiryDate() != null) {
            form.put("expiryDate", m.getExpiryDate().toString());
        }
        return form;
    }
}
