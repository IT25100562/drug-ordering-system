package com.medisys.servlet.prescription;

import com.medisys.model.Prescription;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.sql.SQLException;

/**
 * Sends an uploaded prescription file, after checking who is asking.
 *
 *   GET /prescriptions/file?id=12              show in the browser
 *   GET /prescriptions/file?id=12&download=1   save as a file
 *
 * Only the customer who uploaded it, or a pharmacist, gets the file. Everyone
 * else gets "404 Not Found", so they cannot even tell that it exists.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet("/prescriptions/file")
public class PrescriptionFileServlet extends HttpServlet {

    private final PrescriptionService prescriptionService = new PrescriptionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));

        Prescription p;
        try {
            p = id == null ? null : prescriptionService.getForFileAccess(user, id);
        } catch (SQLException e) {
            throw new ServletException("Could not load the prescription", e);
        }
        if (p == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (InputStream in = prescriptionService.openFile(p)) {
            boolean download = "1".equals(request.getParameter("download"));
            String fileName = URLEncoder.encode(p.getOriginalFileName(), StandardCharsets.UTF_8).replace("+", "%20");

            // The type comes from our database (checked at upload), never from the file name.
            response.setContentType(p.getContentType());
            response.setContentLength(p.getFileSize());
            response.setHeader("Content-Disposition",
                    (download ? "attachment" : "inline") + "; filename*=UTF-8''" + fileName);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "private, no-store");

            OutputStream out = response.getOutputStream();
            in.transferTo(out);
        } catch (NoSuchFileException e) {
            log("Prescription file missing: " + p.getFileKey());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
