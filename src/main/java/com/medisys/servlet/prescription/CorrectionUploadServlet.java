package com.medisys.servlet.prescription;

import com.medisys.model.Prescription;
import com.medisys.model.User;
import com.medisys.service.PrescriptionService;
import com.medisys.service.ValidationException;
import com.medisys.util.SessionUtil;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Customer sends a corrected copy (only when the pharmacist asked for one).
 *
 *   GET  /prescriptions/correct?id=12
 *   POST /prescriptions/correct          id, file
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/prescriptions/correct")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 10L * 1024 * 1024,
        maxRequestSize = 11L * 1024 * 1024)
public class CorrectionUploadServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/prescription/correct.jsp";

    private final PrescriptionService prescriptionService = new PrescriptionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            Prescription p = id == null ? null : prescriptionService.getOwnPrescription(user, id);
            if (p == null) {
                throw new ValidationException("That prescription was not found.");
            }
            if (!p.needsCorrection()) {
                SessionUtil.flash(request, "info", p.getReference() + " does not need a new copy ("
                        + p.getStatus().getLabel() + ").");
                response.sendRedirect(request.getContextPath() + "/prescriptions?highlight=" + p.getId());
                return;
            }
            showForm(request, response, p, null);
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/prescriptions");
        } catch (SQLException e) {
            throw new ServletException("Could not load the prescription", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            Prescription p;
            try {
                UploadedFile.checkSize(request);
            } catch (ValidationException e) {
                // The id could not be read from the rejected request.
                SessionUtil.flash(request, "error", e.getMessage());
                response.sendRedirect(request.getContextPath() + "/prescriptions");
                return;
            }
            Integer id = TextUtil.parseInt(request.getParameter("id"));
            try {
                if (id == null) {
                    throw new ValidationException("That prescription was not found.");
                }
                p = prescriptionService.getOwnPrescription(user, id);
                if (!p.needsCorrection()) {
                    throw new ValidationException(p.getReference() + " does not need a new copy ("
                            + p.getStatus().getLabel() + ").");
                }
            } catch (ValidationException e) {
                SessionUtil.flash(request, "error", e.getMessage());
                response.sendRedirect(request.getContextPath() + "/prescriptions");
                return;
            }

            try {
                UploadedFile file = UploadedFile.read(request);
                prescriptionService.uploadCorrection(user, id, file.name, file.data);
                SessionUtil.flash(request, "success", "Thank you. The new copy of " + p.getReference()
                        + " was sent to our pharmacist.");
                response.sendRedirect(request.getContextPath() + "/prescriptions?highlight=" + id);
            } catch (ValidationException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                showForm(request, response, p, e.getErrors());
            }
        } catch (SQLException e) {
            throw new ServletException("Could not save the new copy", e);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          Prescription p, java.util.List<String> errors) throws ServletException, IOException {
        request.setAttribute("prescription", p);
        request.setAttribute("errors", errors);
        request.setAttribute("maxFileBytes", PrescriptionService.MAX_FILE_BYTES);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
