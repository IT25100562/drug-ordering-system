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
 * Adds delivered stock to a medicine (the "+ Add" box on the inventory page).
 *
 *   POST /admin/medicines/restock   id=5&quantity=100
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@WebServlet("/admin/medicines/restock")
public class RestockMedicineServlet extends HttpServlet {

    private final MedicineService medicineService = new MedicineService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        String quantity = request.getParameter("quantity");

        try {
            if (id == null) {
                throw new ValidationException("No medicine was selected.");
            }
            Medicine medicine = medicineService.restock(id, quantity);
            SessionUtil.flash(request, "success", "Added " + TextUtil.clean(quantity)
                    + " to the stock of " + medicine.getDisplayName() + ".");
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not update the stock", e);
        }
        InventoryServlet.redirectToInventory(request, response);
    }
}
