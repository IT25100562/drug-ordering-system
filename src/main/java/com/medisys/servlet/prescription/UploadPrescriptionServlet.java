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
import java.util.List;

/**
 * Customer uploads a prescription: just the file and an optional note.
 * The pharmacist reads it and lists the medicines.
 *
 *   GET  /prescriptions/upload
 *   POST /prescriptions/upload   file, note, confirm=yes
 *
 * The Tomcat limits below are a little above 5 MB, so that PrescriptionService
 * can give a friendly "too large" message for files just over the limit.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/prescriptions/upload")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 10L * 1024 * 1024,
        maxRequestSize = 11L * 1024 * 1024)
public class UploadPrescriptionServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/prescription/upload.jsp";

    private final PrescriptionService prescriptionService = new PrescriptionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showForm(request, response, "", null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        String note = "";
        try {
            try {
                UploadedFile.checkSize(request);
                note = TextUtil.clean(request.getParameter("note"));
                UploadedFile file = UploadedFile.read(request);
                if (!"yes".equals(request.getParameter("confirm"))) {
                    throw new ValidationException("Please confirm that the prescription was issued to you.");
                }

                int id = prescriptionService.upload(user, note, file.name, file.data);
                Prescription saved = prescriptionService.getOwnPrescription(user, id);
                SessionUtil.flash(request, "success", "Prescription " + saved.getReference()
                        + " was uploaded. Our pharmacist will check it, list your medicines and notify you.");
                response.sendRedirect(request.getContextPath() + "/prescriptions?highlight=" + id);
            } catch (ValidationException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                showForm(request, response, note, e.getErrors());
            }
        } catch (SQLException e) {
            throw new ServletException("Could not save the prescription", e);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          String note, List<String> errors) throws ServletException, IOException {
        request.setAttribute("note", note);
        request.setAttribute("errors", errors);
        request.setAttribute("maxFileBytes", PrescriptionService.MAX_FILE_BYTES);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
