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
import java.sql.SQLException;

/**
 * The Reports and Analytics page (admin).
 *
 *   GET /admin/reports                     last 30 days
 *   GET /admin/reports?range=month         a preset (7, 30, 90, month, lastmonth, year)
 *   GET /admin/reports?from=2026-09-01&to=2026-09-17
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/admin/reports")
public class ReportsServlet extends HttpServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ReportPeriod period;
        try {
            period = reportService.resolvePeriod(request.getParameter("range"),
                    request.getParameter("from"), request.getParameter("to"));
        } catch (ValidationException e) {
            // Bad custom dates: say so and show the default period instead.
            SessionUtil.flash(request, "error", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/reports");
            return;
        }
        try {
            request.setAttribute("report", reportService.buildReport(period));
        } catch (SQLException e) {
            throw new ServletException("Could not build the report", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/report/dashboard.jsp").forward(request, response);
    }
}
