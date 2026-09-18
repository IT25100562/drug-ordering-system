package com.medisys.servlet.report;

import com.medisys.model.ReportPeriod;
import com.medisys.service.ReportService;
import com.medisys.service.ValidationException;
import com.medisys.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Downloads one report table as a CSV file (opens in Excel).
 *
 *   GET /admin/reports/export?type=medicines&from=2026-09-01&to=2026-09-17
 *   (type: summary, daily, medicines, categories, customers, prescriptions,
 *          riders, lowstock, expiring)
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/admin/reports/export")
public class ExportReportServlet extends HttpServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String type = request.getParameter("type");
        try {
            ReportPeriod period = reportService.resolvePeriod(request.getParameter("range"),
                    request.getParameter("from"), request.getParameter("to"));
            String csv = reportService.exportCsv(type, period);

            response.setContentType("text/csv");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + reportService.exportFileName(type, period) + "\"");
            response.setHeader("Cache-Control", "private, no-store");
            // The "byte order mark" tells Excel the file is UTF-8 (names like "Nuwan's" stay correct).
            response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/reports");
        } catch (SQLException e) {
            throw new ServletException("Could not export the report", e);
        }
    }
}
