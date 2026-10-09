package com.medisys.report;

import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.ValidationException;
import com.medisys.user.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;

/**
 * Saved reports: the full CRUD of the Reports module.
 *
 *   GET  /admin/reports/saved                   all saved reports                READ
 *   GET  /admin/reports/saved/view?id=5         one, next to today's numbers     READ
 *   POST /admin/reports/saved  action=create    save the numbers of a period     CREATE
 *   POST /admin/reports/saved  action=update    change the title and notes       UPDATE
 *   POST /admin/reports/saved  action=delete    delete it                        DELETE
 *
 * "Save this report" keeps the headline numbers of a period with a title
 * (3-100 characters) and optional notes (up to 500), so months can be
 * compared later. The numbers of a saved report are never changed.
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet({"/admin/reports/saved", "/admin/reports/saved/view"})
public class SavedReportServlet extends HttpServlet {

    private final SavedReportDAO savedReportDAO = new SavedReportDAO();
    private final ReportDAO reportDAO = new ReportDAO();

    // ================================================================== READ

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/admin/reports/saved")) {
                request.setAttribute("savedReports", savedReportDAO.getAllSavedReports());
                request.getRequestDispatcher("/WEB-INF/views/report/saved-reports.jsp").forward(request, response);
                return;
            }
            Integer id = TextUtil.parseInt(request.getParameter("id"));
            SavedReport saved = id == null ? null : savedReportDAO.getSavedReportById(id);
            if (saved == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("saved", saved);
            // Today's numbers for the same period, to see what changed since it was saved.
            request.setAttribute("live", reportDAO.getSummary(saved.getPeriod()));
            request.getRequestDispatcher("/WEB-INF/views/report/saved-report.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load the saved reports", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = TextUtil.clean(request.getParameter("action"));
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        String back = "/admin/reports/saved";
        try {
            if ("create".equals(action)) {
                int newId = create(request);
                SessionUtil.flash(request, "success", "The report was saved.");
                back = "/admin/reports/saved/view?id=" + newId;
            } else if ("update".equals(action) && id != null) {
                // UPDATE
                String title = checkTitle(request.getParameter("title"));
                String notes = checkNotes(request.getParameter("notes"));
                if (!savedReportDAO.updateSavedReport(id, title, notes)) {
                    throw new ValidationException("That saved report no longer exists.");
                }
                SessionUtil.flash(request, "success", "The saved report was updated.");
                back = "/admin/reports/saved/view?id=" + id;
            } else if ("delete".equals(action) && id != null) {
                // DELETE
                SavedReport r = savedReportDAO.getSavedReportById(id);
                if (r == null || !savedReportDAO.deleteSavedReport(id)) {
                    throw new ValidationException("That saved report no longer exists.");
                }
                SessionUtil.flash(request, "success", "\"" + r.getTitle() + "\" was deleted.");
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

    // ================================================================ CREATE

    /** Saves the headline numbers of the period with a title and notes. Returns the new id. */
    private int create(HttpServletRequest request) throws SQLException, ValidationException {
        User admin = SessionUtil.currentUser(request);
        ReportPeriod period = ReportServlet.resolvePeriod(null, request.getParameter("from"), request.getParameter("to"));
        String title = checkTitle(request.getParameter("title"));
        String notes = checkNotes(request.getParameter("notes"));

        ReportSummary s = reportDAO.getSummary(period);
        SavedReport r = new SavedReport();
        r.setTitle(title);
        r.setNotes(notes);
        r.setPeriodFrom(period.getFrom());
        r.setPeriodTo(period.getTo());
        r.setRevenue(s.getRevenue());
        r.setOrderCount(s.getOrderCount());
        r.setAverageOrder(s.getAverageOrder());
        r.setCancelledCount(s.getCancelledCount());
        r.setRefundedAmount(s.getRefundedAmount());
        r.setPrescriptionCount(s.getPrescriptionCount());
        r.setApprovalRate(toDecimal(s.getApprovalRate()));
        r.setDeliveredCount(s.getDeliveredCount());
        r.setOnTimeRate(toDecimal(s.getOnTimeRate()));
        r.setNewCustomers(s.getNewCustomers());
        r.setCreatedBy(admin.getId());
        return savedReportDAO.addSavedReport(r);
    }

    // ============================================================ VALIDATION

    private String checkTitle(String text) throws ValidationException {
        String title = TextUtil.clean(text).replaceAll("\\s+", " ");
        if (title.length() < SavedReport.TITLE_MIN || title.length() > SavedReport.TITLE_MAX) {
            throw new ValidationException("Please give the report a title (" + SavedReport.TITLE_MIN + " to "
                    + SavedReport.TITLE_MAX + " characters), e.g. \"September 2026 sales\".");
        }
        return title;
    }

    private String checkNotes(String text) throws ValidationException {
        String notes = TextUtil.clean(text);
        if (notes.length() > SavedReport.NOTES_MAX) {
            throw new ValidationException("The notes can have at most " + SavedReport.NOTES_MAX + " characters.");
        }
        return notes.isEmpty() ? null : notes;
    }

    private static BigDecimal toDecimal(Double percent) {
        return percent == null ? null : BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP);
    }
}
