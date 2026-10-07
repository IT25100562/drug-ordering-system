package com.medisys.servlet.prescription;

import com.medisys.model.Prescription;
import com.medisys.service.PrescriptionService;
import com.medisys.service.UserService;
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
import java.util.List;

/**
 * Review one prescription and Approve (with the medicines) / Reject /
 * Request correction.
 *
 *   GET  /pharmacist/review?id=12
 *   POST /pharmacist/review   id, decision (APPROVE | REJECT | CORRECTION), note,
 *                             medicineId[], quantity[], dosage[]   (one per medicine line)
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/pharmacist/review")
public class ReviewPrescriptionServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/prescription/review.jsp";

    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            Prescription p = id == null ? null : prescriptionService.getPrescription(id);
            if (p == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            showPage(request, response, p, "", new String[0], new String[0], new String[0], null);
        } catch (SQLException e) {
            throw new ServletException("Could not load the prescription", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        String note = TextUtil.clean(request.getParameter("note"));
        String[] medicineIds = request.getParameterValues("medicineId");
        String[] quantities = request.getParameterValues("quantity");
        String[] dosages = request.getParameterValues("dosage");
        try {
            if (id == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            try {
                String message = prescriptionService.decide(SessionUtil.currentUser(request), id,
                        request.getParameter("decision"), note, medicineIds, quantities, dosages);
                SessionUtil.flash(request, "success", message);
                response.sendRedirect(request.getContextPath() + "/pharmacist/dashboard");
            } catch (ValidationException e) {
                Prescription p = prescriptionService.getPrescription(id);
                if (p == null) {
                    SessionUtil.flash(request, "error", e.getMessage());
                    response.sendRedirect(request.getContextPath() + "/pharmacist/dashboard");
                    return;
                }
                // Show the page again with the messages and everything that was typed.
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                showPage(request, response, p, note, medicineIds, quantities, dosages, e.getErrors());
            }
        } catch (SQLException e) {
            throw new ServletException("Could not save the decision", e);
        }
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response, Prescription p,
                          String note, String[] medicineIds, String[] quantities, String[] dosages,
                          List<String> errors) throws ServletException, IOException, SQLException {
        request.setAttribute("prescription", p);
        request.setAttribute("note", note);
        request.setAttribute("rowMedicineIds", medicineIds == null ? new String[0] : medicineIds);
        request.setAttribute("rowQuantities", quantities == null ? new String[0] : quantities);
        request.setAttribute("rowDosages", dosages == null ? new String[0] : dosages);
        request.setAttribute("errors", errors);
        // Who uploaded it: photo, NIC, age, contact, red flag and history (module 04).
        request.setAttribute("customer", userService.getUser(p.getUserId()));
        request.setAttribute("customerStats", userService.getCustomerStats(p.getUserId()));
        if (p.isAwaitingReview()) {
            request.setAttribute("medicines", prescriptionService.getMedicinesForPrescribing());
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
