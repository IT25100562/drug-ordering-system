package com.medisys.servlet.medicine;

import com.medisys.model.Medicine;
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
 * Details page for one medicine.
 *
 *   GET /medicines/view?id=5
 *
 * Discontinued and expired medicines are shown as "not found" to customers.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet("/medicines/view")
public class MedicineDetailsServlet extends HttpServlet {

    private final MedicineService medicineService = new MedicineService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Medicine medicine;
        try {
            medicine = id == null ? null : medicineService.getCatalogMedicine(id);
        } catch (SQLException e) {
            throw new ServletException("Could not load the medicine", e);
        }
        if (medicine == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        request.setAttribute("medicine", medicine);
        request.getRequestDispatcher("/WEB-INF/views/medicine/medicine-details.jsp").forward(request, response);
    }
}
