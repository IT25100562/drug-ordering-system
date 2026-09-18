package com.medisys.servlet.report;

import com.medisys.model.ReportPeriod;
import com.medisys.model.User;
import com.medisys.service.ReportService;
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
 * Saved reports: list them, save a new one, edit its title / notes, delete it.
 *
 *   GET  /admin/reports/saved
 *   POST /admin/reports/saved   action=create  from, to, title, notes
 *   POST /admin/reports/saved   action=update  id, title, notes
 *   POST /admin/reports/saved   action=delete  id
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/admin/reports/saved")
public class SavedReportsServlet extends HttpServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("savedReports", reportService.getSavedReports());
        } catch (SQLException e) {
            throw new ServletException("Could not load the saved reports", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/report/saved-reports.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User admin = SessionUtil.currentUser(request);
        String action = TextUtil.clean(request.getParameter("action"));
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        String back = "/admin/reports/saved";
        try {
            if ("create".equals(action)) {
                ReportPeriod period = reportService.resolvePeriod(null,
                        request.getParameter("from"), request.getParameter("to"));
                int newId = reportService.saveReport(admin, period,
                        request.getParameter("title"), request.getParameter("notes"));
                SessionUtil.flash(request, "success", "The report was saved.");
                back = "/admin/reports/saved/view?id=" + newId;
            } else if ("update".equals(action) && id != null) {
                SessionUtil.flash(request, "success",
                        reportService.updateSavedReport(id, request.getParameter("title"), request.getParameter("notes")));
                back = "/admin/reports/saved/view?id=" + id;
            } else if ("delete".equals(action) && id != null) {
                SessionUtil.flash(request, "success", reportService.deleteSavedReport(id));
            } else {
                throw new ValidationException("Unknown action.");
            }
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
            // Go back to the page the form was on.
            String returnTo = request.getParameter("returnTo");
            if (TextUtil.isSafeLocalPath(returnTo)) {
                back = returnTo;
            }
        } catch (SQLException e) {
            throw new ServletException("Could not save the report", e);
        }
        response.sendRedirect(request.getContextPath() + back);
    }
}
