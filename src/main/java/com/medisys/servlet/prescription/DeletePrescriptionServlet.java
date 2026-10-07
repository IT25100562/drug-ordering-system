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
 * Pharmacist deletes an invalid or expired prescription and its stored file.
 *
 *   POST /pharmacist/delete   id=12&returnTo=/pharmacist/dashboard?status=EXPIRED
 *
 * A prescription used for an order is refused.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/pharmacist/delete")
public class DeletePrescriptionServlet extends HttpServlet {

    private final PrescriptionService prescriptionService = new PrescriptionService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            if (id == null) {
                throw new ValidationException("No prescription was selected.");
            }
            Prescription p = prescriptionService.delete(SessionUtil.currentUser(request), id);
            SessionUtil.flash(request, "success", p.getReference() + " and its file were deleted. "
                    + "The customer has been notified.");
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not delete the prescription", e);
        }

        String back = request.getParameter("returnTo");
        // The review page of a deleted prescription no longer exists, so go to the dashboard.
        if (!TextUtil.isSafeLocalPath(back) || back.startsWith("/pharmacist/review")) {
            back = "/pharmacist/dashboard";
        }
        response.sendRedirect(request.getContextPath() + back);
    }
}
