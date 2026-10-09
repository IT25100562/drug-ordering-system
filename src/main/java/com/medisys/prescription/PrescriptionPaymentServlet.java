package com.medisys.prescription;

import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.ValidationException;
import com.medisys.order.CheckoutServlet;
import com.medisys.order.Order;
import com.medisys.order.OrderItem;
import com.medisys.user.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The customer pays for an approved prescription.
 *
 *   GET  /prescriptions/pay?id=5    delivery details + test card form
 *   POST /prescriptions/pay         pay: this creates an order (module 02)
 *
 * Only an approved, unpaid, not expired prescription can be paid. Paying
 * creates an order with the medicines and prices the pharmacist approved, so
 * stock is taken out at the same moment (all medicines or none) and it is
 * delivered like any other order.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/prescriptions/pay")
public class PrescriptionPaymentServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/prescription/pay.jsp";

    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            Prescription p = loadPayable(request, response, user);
            if (p != null) {
                showForm(request, response, p, CheckoutServlet.startForm(user), null);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not load the payment page", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        Map<String, String> form = CheckoutServlet.readForm(request);
        try {
            Prescription p = loadPayable(request, response, user);
            if (p == null) {
                return;
            }
            // The order lines are the medicines, prices and "how to use" the pharmacist approved.
            List<OrderItem> items = new ArrayList<>();
            for (PrescriptionItem line : p.getItems()) {
                items.add(new OrderItem(line.getMedicine(), line.getUnitPrice(), line.getQuantity(),
                        line.getDosageInstructions()));
            }
            try {
                CheckoutServlet.placeOrder(user, Order.SOURCE_PRESCRIPTION, items, p.getTotal(), p.getId(), form);
                Prescription paid = prescriptionDAO.getPrescriptionById(p.getId());
                SessionUtil.flash(request, "success", "Payment successful. Order " + paid.getOrderReference()
                        + " (reference " + paid.getPaymentReference() + ") is being prepared for delivery.");
                response.sendRedirect(request.getContextPath() + "/prescriptions/view?id=" + p.getId());
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
    private Prescription loadPayable(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException, SQLException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Prescription p = id == null ? null : prescriptionDAO.getPrescriptionById(id);
        if (p == null || p.getUserId() != user.getId()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        String problem = null;
        if (p.isPaid()) {
            problem = p.getReference() + " is already paid.";
        } else if (p.getStatus() != PrescriptionStatus.APPROVED) {
            problem = p.getReference() + " can only be paid after our pharmacist approves it.";
        } else if (p.isExpired()) {
            problem = p.getReference() + " has expired. Please upload a new prescription.";
        } else if (p.getItems().isEmpty()) {
            problem = p.getReference() + " has no medicines to pay for.";
        }
        if (problem != null) {
            SessionUtil.flash(request, p.isPaid() ? "info" : "error", problem);
            response.sendRedirect(request.getContextPath() + "/prescriptions/view?id=" + id);
            return null;
        }
        return p;
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Prescription p,
                          Map<String, String> form, List<String> errors) throws ServletException, IOException {
        request.setAttribute("prescription", p);
        request.setAttribute("deliveryFee", Order.deliveryFeeFor(p.getTotal()));
        request.setAttribute("total", Order.totalFor(p.getTotal()));
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
