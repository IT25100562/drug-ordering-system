package com.medisys.servlet.report;

import com.medisys.model.SavedReport;
import com.medisys.service.ReportService;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * One saved report: the frozen numbers, next to the live numbers of the same
 * period (so changes since saving are visible), with the edit form.
 *
 *   GET /admin/reports/saved/view?id=3
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/admin/reports/saved/view")
public class SavedReportServlet extends HttpServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        try {
            SavedReport saved = id == null ? null : reportService.getSavedReport(id);
            if (saved == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("saved", saved);
            request.setAttribute("live", reportService.getSummary(saved.getPeriod()));
        } catch (SQLException e) {
            throw new ServletException("Could not load the saved report", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/report/saved-report.jsp").forward(request, response);
    }
}
