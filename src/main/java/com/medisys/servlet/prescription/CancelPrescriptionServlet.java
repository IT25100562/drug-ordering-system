package com.medisys.servlet.prescription;

import com.medisys.model.Prescription;
import com.medisys.service.PrescriptionService;
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
 * Customer deletes one of their own prescriptions (not a paid one).
 *
 *   POST /prescriptions/delete   id=12
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/prescriptions/delete")
public class CancelPrescriptionServlet extends HttpServlet {

    private final PrescriptionService prescriptionService = new PrescriptionService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            if (id == null) {
                throw new ValidationException("That prescription was not found.");
            }
            Prescription p = prescriptionService.delete(SessionUtil.currentUser(request), id);
            SessionUtil.flash(request, "success", "Prescription " + p.getReference() + " was deleted.");
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not delete the prescription", e);
        }
        response.sendRedirect(request.getContextPath() + "/prescriptions");
    }
}
