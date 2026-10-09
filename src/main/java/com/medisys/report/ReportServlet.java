package com.medisys.report;

import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.ValidationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The admin's reports (READ only: the numbers are added up from the other
 * modules' tables).
 *
 *   GET /admin/reports?range=30                      a preset period
 *   GET /admin/reports?from=2026-09-01&to=2026-09-30 custom dates
 *   GET /admin/reports/export?type=daily&range=30    one table as a CSV file (opens in Excel)
 *
 * Rules:
 *  - presets: last 7 / 30 / 90 days, this month, last month, this year
 *  - custom dates must be real dates, "from" not after "to", not in the
 *    future, and at most 366 days long
 *  - sales, prescriptions, deliveries and customers are counted for the
 *    period; the inventory part always shows the stock as it is now
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet({"/admin/reports", "/admin/reports/export"})
public class ReportServlet extends HttpServlet {

    private static final int MAX_DAYS = 366;

    /** The CSV downloads: type -> file name part. */
    private static final Map<String, String> EXPORTS = new LinkedHashMap<>();

    static {
        EXPORTS.put("summary", "summary");
        EXPORTS.put("daily", "daily-sales");
        EXPORTS.put("medicines", "top-medicines");
        EXPORTS.put("categories", "sales-by-category");
        EXPORTS.put("customers", "top-customers");
        EXPORTS.put("prescriptions", "prescriptions");
        EXPORTS.put("riders", "rider-performance");
        EXPORTS.put("lowstock", "low-stock");
        EXPORTS.put("expiring", "expiring-soon");
    }

    private final ReportDAO reportDAO = new ReportDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            ReportPeriod period = resolvePeriod(request.getParameter("range"),
                    request.getParameter("from"), request.getParameter("to"));
            if (request.getServletPath().equals("/admin/reports/export")) {
                exportCsv(request.getParameter("type"), period, response);
                return;
            }
            request.setAttribute("report", buildReport(period));
            request.getRequestDispatcher("/WEB-INF/views/report/dashboard.jsp").forward(request, response);
        } catch (ValidationException e) {
            // Bad custom dates or export type: say so and show the default period instead.
            SessionUtil.flash(request, "error", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/admin/reports");
        } catch (SQLException e) {
            throw new ServletException("Could not build the report", e);
        }
    }

    /**
     * VALIDATION: works out the period from the page's parameters.
     * Custom dates (from / to) win over a preset. Also used by SavedReportServlet.
     */
    static ReportPeriod resolvePeriod(String presetText, String fromText, String toText) throws ValidationException {
        if (TextUtil.isBlank(fromText) && TextUtil.isBlank(toText)) {
            return ReportPeriod.preset(ReportPeriod.PRESETS.containsKey(presetText) ? presetText : ReportPeriod.DEFAULT_PRESET);
        }
        LocalDate from = TextUtil.parseDate(fromText);
        LocalDate to = TextUtil.parseDate(toText);
        if (from == null || to == null) {
            throw new ValidationException("Please choose both a start date and an end date.");
        }
        if (from.isAfter(to)) {
            throw new ValidationException("The start date must be on or before the end date.");
        }
        if (to.isAfter(LocalDate.now())) {
            throw new ValidationException("The end date can't be in the future.");
        }
        if (from.isBefore(LocalDate.of(2020, 1, 1))) {
            throw new ValidationException("Reports start from 1 January 2020.");
        }
        ReportPeriod custom = new ReportPeriod(from, to, "custom");
        if (custom.getDays() > MAX_DAYS) {
            throw new ValidationException("A report can cover at most " + MAX_DAYS + " days.");
        }
        // Custom dates that happen to equal a preset are shown as that preset (highlights its button).
        for (String key : ReportPeriod.PRESETS.keySet()) {
            ReportPeriod p = ReportPeriod.preset(key);
            if (p.getFrom().equals(from) && p.getTo().equals(to)) {
                return p;
            }
        }
        return custom;
    }

    /** Everything on the Reports page for one period. */
    private Report buildReport(ReportPeriod period) throws SQLException {
        Report report = new Report();
        report.setPeriod(period);
        report.setSummary(reportDAO.getSummary(period));
        report.setDailySales(fillMissingDays(period, reportDAO.getDailySales(period)));
        report.setTopMedicines(reportDAO.getTopMedicines(period, Report.TOP_MEDICINES));
        report.setCategories(reportDAO.getSalesByCategory(period));
        report.setTopCustomers(reportDAO.getTopCustomers(period, Report.TOP_CUSTOMERS));
        report.setPrescriptionStatuses(reportDAO.getPrescriptionStatuses(period));
        report.setRiders(reportDAO.getRiderPerformance(period));
        report.setLowStock(reportDAO.getLowStock());
        report.setExpiringSoon(reportDAO.getExpiringSoon(Report.EXPIRY_WARNING_DAYS));
        report.setStockValue(reportDAO.getStockValue());
        report.setOutOfStockCount(reportDAO.countOutOfStock());
        return report;
    }

    /** The database only returns days that had sales; the chart needs every day, so the others get 0. */
    private List<ReportRow> fillMissingDays(ReportPeriod period, List<ReportRow> salesDays) {
        Map<String, ReportRow> byDay = new HashMap<>();
        for (ReportRow row : salesDays) {
            byDay.put(row.getLabel(), row);
        }
        List<ReportRow> all = new ArrayList<>();
        for (LocalDate day = period.getFrom(); !day.isAfter(period.getTo()); day = day.plusDays(1)) {
            ReportRow row = byDay.get(day.toString());
            all.add(row != null ? row : new ReportRow(day.toString(), null, 0, BigDecimal.ZERO.setScale(2), 0));
        }
        return all;
    }

    // ================================================================ export

    /** Sends one report table as a CSV file. */
    private void exportCsv(String type, ReportPeriod period, HttpServletResponse response)
            throws SQLException, IOException, ValidationException {
        if (!EXPORTS.containsKey(type)) {
            throw new ValidationException("Unknown report.");
        }
        Report r = buildReport(period);
        StringBuilder csv = new StringBuilder();
        line(csv, "MediSys report", EXPORTS.get(type), period.getFrom().toString(), period.getTo().toString());
        switch (type) {
            case "summary": {
                ReportSummary s = r.getSummary();
                line(csv, "Measure", "Value");
                line(csv, "Revenue (Rs.)", s.getRevenue().toPlainString());
                line(csv, "Orders", String.valueOf(s.getOrderCount()));
                line(csv, "Average order (Rs.)", s.getAverageOrder().toPlainString());
                line(csv, "Revenue from the cart (Rs.)", s.getCartRevenue().toPlainString());
                line(csv, "Revenue from prescriptions (Rs.)", s.getPrescriptionRevenue().toPlainString());
                line(csv, "Delivery fees (Rs.)", s.getDeliveryFees().toPlainString());
                line(csv, "Cancelled orders", String.valueOf(s.getCancelledCount()));
                line(csv, "Refunded (Rs.)", s.getRefundedAmount().toPlainString());
                line(csv, "New customers", String.valueOf(s.getNewCustomers()));
                line(csv, "Customers who ordered", String.valueOf(s.getBuyingCustomers()));
                line(csv, "Prescriptions uploaded", String.valueOf(s.getPrescriptionCount()));
                line(csv, "Approval rate (%)", percent(s.getApprovalRate()));
                line(csv, "Average review time (hours)", s.getAverageReviewHours() == null ? ""
                        : String.format("%.1f", s.getAverageReviewHours()));
                line(csv, "Deliveries completed", String.valueOf(s.getDeliveredCount()));
                line(csv, "On time (%)", percent(s.getOnTimeRate()));
                line(csv, "Failed delivery attempts", String.valueOf(s.getFailedAttempts()));
                break;
            }
            case "daily":
                line(csv, "Date", "Orders", "Revenue (Rs.)");
                for (ReportRow row : r.getDailySales()) {
                    line(csv, row.getLabel(), String.valueOf(row.getCount()), row.getAmount().toPlainString());
                }
                break;
            case "medicines":
                line(csv, "Medicine", "Category", "Packs sold", "Revenue (Rs.)");
                for (ReportRow row : r.getTopMedicines()) {
                    line(csv, row.getLabel(), row.getDetail(), String.valueOf(row.getCount()), row.getAmount().toPlainString());
                }
                break;
            case "categories":
                line(csv, "Category", "Packs sold", "Revenue (Rs.)");
                for (ReportRow row : r.getCategories()) {
                    line(csv, row.getLabel(), String.valueOf(row.getCount()), row.getAmount().toPlainString());
                }
                break;
            case "customers":
                line(csv, "Customer", "Orders", "Spent (Rs.)");
                for (ReportRow row : r.getTopCustomers()) {
                    line(csv, row.getLabel(), String.valueOf(row.getCount()), row.getAmount().toPlainString());
                }
                break;
            case "prescriptions":
                line(csv, "Status", "Prescriptions");
                for (ReportRow row : r.getPrescriptionStatuses()) {
                    line(csv, row.getDetail(), String.valueOf(row.getCount()));
                }
                break;
            case "riders":
                line(csv, "Rider", "Delivered", "On time", "On time (%)", "Failed attempts");
                for (ReportRow row : r.getRiders()) {
                    line(csv, row.getLabel(), String.valueOf(row.getCount()), String.valueOf(row.getExtra()),
                            percent(row.getExtraPercent()), row.getDetail());
                }
                break;
            case "lowstock":
                line(csv, "Medicine", "Category", "In stock", "Reorder level");
                for (ReportRow row : r.getLowStock()) {
                    line(csv, row.getLabel(), row.getDetail(), String.valueOf(row.getCount()), String.valueOf(row.getExtra()));
                }
                break;
            default:
                line(csv, "Medicine", "Expiry date", "In stock");
                for (ReportRow row : r.getExpiringSoon()) {
                    line(csv, row.getLabel(), row.getDetail(), String.valueOf(row.getCount()));
                }
        }

        String fileName = "medisys-" + EXPORTS.get(type) + "-" + period.getFrom() + "-to-" + period.getTo() + ".csv";
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Cache-Control", "private, no-store");
        // The "byte order mark" tells Excel the file is UTF-8 (names like "Nuwan's" stay correct).
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        response.getOutputStream().write(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Adds one CSV line. Values are quoted, and quotes inside are doubled. */
    private static void line(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            String v = values[i] == null ? "" : values[i];
            // A value starting with = + - @ would be run as a formula by Excel
            // ("CSV injection"), so it gets a ' in front. Numbers like -5 are kept.
            if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !v.matches("-?\\d+(\\.\\d+)?")) {
                v = "'" + v;
            }
            csv.append('"').append(v.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private static String percent(Double value) {
        return value == null ? "" : String.format("%.1f", value);
    }
}
