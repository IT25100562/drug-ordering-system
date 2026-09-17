package com.medisys.servlet.prescription;

import com.medisys.model.User;
import com.medisys.service.PrescriptionService;
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
 * Customer's list of submitted prescriptions.
 *
 *   GET /prescriptions?highlight=12   (highlight = the one just uploaded)
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/prescriptions")
public class MyPrescriptionsServlet extends HttpServlet {

    private final PrescriptionService prescriptionService = new PrescriptionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            request.setAttribute("prescriptions", prescriptionService.getMyPrescriptions(user.getId()));
        } catch (SQLException e) {
            throw new ServletException("Could not load your prescriptions", e);
        }
        request.setAttribute("highlight", TextUtil.parseInt(request.getParameter("highlight")));
        request.getRequestDispatcher("/WEB-INF/views/prescription/my-prescriptions.jsp").forward(request, response);
    }
}
