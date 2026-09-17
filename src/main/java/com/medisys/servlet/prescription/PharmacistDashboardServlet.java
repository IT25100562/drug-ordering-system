package com.medisys.servlet.prescription;

import com.medisys.dao.PrescriptionDAO;
import com.medisys.model.PrescriptionStatus;
import com.medisys.service.PrescriptionService;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Verification Dashboard: the senior pharmacist's queue of prescriptions.
 *
 *   GET /pharmacist/dashboard                  waiting for verification
 *   GET /pharmacist/dashboard?status=EXPIRED   APPROVED / PAID / REJECTED / CORRECTION_REQUESTED / EXPIRED / ALL
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/pharmacist/dashboard")
public class PharmacistDashboardServlet extends HttpServlet {

    private final PrescriptionService prescriptionService = new PrescriptionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String filter = TextUtil.clean(request.getParameter("status")).toUpperCase();
        boolean known = PrescriptionStatus.fromText(filter) != null
                || PrescriptionDAO.FILTER_PAID.equals(filter)
                || PrescriptionDAO.FILTER_EXPIRED.equals(filter) || PrescriptionDAO.FILTER_ALL.equals(filter);
        if (!known) {
            filter = PrescriptionStatus.PENDING.name();
        }

        try {
            request.setAttribute("prescriptions", prescriptionService.getDashboard(filter));
            request.setAttribute("counts", prescriptionService.getDashboardCounts());
        } catch (SQLException e) {
            throw new ServletException("Could not load the dashboard", e);
        }
        request.setAttribute("filter", filter);
        request.getRequestDispatcher("/WEB-INF/views/prescription/dashboard.jsp").forward(request, response);
    }
}
