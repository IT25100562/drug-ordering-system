package com.medisys.servlet.prescription;

import com.medisys.model.Prescription;
import com.medisys.model.User;
import com.medisys.service.OrderService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test payment for an approved prescription.
 *
 *   GET  /prescriptions/pay?id=12
 *   POST /prescriptions/pay        id + the checkout fields (OrderService.FORM_FIELDS)
 *
 * Paying creates an order (module 02) with the approved medicines.
 *
 * The card number and CVV are only checked, never stored or sent back to the
 * page (after an error the card fields are empty again).
 *
 * Module : 05 - Prescription Upload and Verification
 *          (the card check is the module 02 placeholder in PaymentService)
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/prescriptions/pay")
public class PrescriptionPaymentServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/prescription/pay.jsp";

    private final PrescriptionService prescriptionService = new PrescriptionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            Prescription p = load(request, response, user, id);
            if (p == null) {
                return;
            }
            // Start with the customer's own details.
            Map<String, String> form = new HashMap<>();
            form.put("deliveryName", user.getFullName());
            form.put("deliveryAddress", user.getAddress());
            form.put("deliveryPhone", user.getPhone());
            form.put("cardName", user.getFullName());
            showForm(request, response, p, form, null);
        } catch (SQLException e) {
            throw new ServletException("Could not load the payment page", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Map<String, String> form = new HashMap<>();
        for (String field : OrderService.FORM_FIELDS) {
            form.put(field, request.getParameter(field));
        }

        try {
            Prescription p = load(request, response, user, id);
            if (p == null) {
                return;
            }
            try {
                Prescription paid = prescriptionService.pay(user, id, form);
                SessionUtil.flash(request, "success", "Payment successful. Order " + paid.getOrderReference()
                        + " (reference " + paid.getPaymentReference() + ") is being prepared for delivery.");
                response.sendRedirect(request.getContextPath() + "/prescriptions/view?id=" + id);
            } catch (ValidationException e) {
                // Never show the card number or CVV again.
                form.remove("cardNumber");
                form.remove("cardCvv");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                showForm(request, response, p, form, e.getErrors());
            }
        } catch (SQLException e) {
            throw new ServletException("Could not process the payment", e);
        }
    }

    /**
     * The customer's payable prescription, or null after sending the customer
     * somewhere sensible (404, or back to the prescription with a message).
     */
    private Prescription load(HttpServletRequest request, HttpServletResponse response, User user, Integer id)
            throws IOException, SQLException {
        if (id == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        Prescription p;
        try {
            p = prescriptionService.getOwnPrescription(user, id);
        } catch (ValidationException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        try {
            prescriptionService.checkPayable(p);
            return p;
        } catch (ValidationException e) {
            SessionUtil.flash(request, p.isPaid() ? "info" : "error", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/prescriptions/view?id=" + id);
            return null;
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Prescription p,
                          Map<String, String> form, List<String> errors) throws ServletException, IOException {
        request.setAttribute("prescription", p);
        request.setAttribute("deliveryFee", OrderService.deliveryFeeFor(p.getTotal()));
        request.setAttribute("total", OrderService.totalFor(p.getTotal()));
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
