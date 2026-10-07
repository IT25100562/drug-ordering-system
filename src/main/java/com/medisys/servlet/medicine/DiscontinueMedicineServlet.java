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

/**
 * Discontinues a medicine (soft delete) or restores it.
 *
 *   POST /admin/medicines/discontinue   id=5&action=discontinue
 *   POST /admin/medicines/discontinue   id=5&action=restore
 *
 * A discontinued medicine is hidden from the catalog but stays in the
 * database, so old orders that point to it keep working.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet("/admin/medicines/discontinue")
public class DiscontinueMedicineServlet extends HttpServlet {

    private final MedicineService medicineService = new MedicineService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        boolean restore = "restore".equals(request.getParameter("action"));

        try {
            if (id == null) {
                throw new ValidationException("No medicine was selected.");
            }
            if (restore) {
                Medicine medicine = medicineService.restore(id);
                SessionUtil.flash(request, "success",
                        medicine.getDisplayName() + " is back in the catalog.");
            } else {
                Medicine medicine = medicineService.discontinue(id);
                SessionUtil.flash(request, "success",
                        medicine.getDisplayName() + " was discontinued and hidden from the catalog.");
            }
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not update the medicine", e);
        }
        InventoryServlet.redirectToInventory(request, response);
    }
}
