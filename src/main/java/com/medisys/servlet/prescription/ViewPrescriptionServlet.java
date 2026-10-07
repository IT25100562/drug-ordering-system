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
 * One of the customer's prescriptions: its status, and once approved, the
 * medicines with how to use them, the total and the Pay button. After payment
 * it works as the receipt.
 *
 *   GET /prescriptions/view?id=12
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/prescriptions/view")
public class ViewPrescriptionServlet extends HttpServlet {

    private final PrescriptionService prescriptionService = new PrescriptionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            if (id == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Prescription p = prescriptionService.getOwnPrescription(SessionUtil.currentUser(request), id);
            request.setAttribute("prescription", p);
            request.getRequestDispatcher("/WEB-INF/views/prescription/view.jsp").forward(request, response);
        } catch (ValidationException e) {
            // Someone else's (or no such) prescription: do not reveal which.
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException("Could not load the prescription", e);
        }
    }
}
