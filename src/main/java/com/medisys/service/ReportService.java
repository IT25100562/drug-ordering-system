package com.medisys.service;

import com.medisys.dao.ReportDAO;
import com.medisys.dao.SavedReportDAO;
import com.medisys.dao.impl.ReportDAOImpl;
import com.medisys.dao.impl.SavedReportDAOImpl;
import com.medisys.model.Report;
import com.medisys.model.ReportPeriod;
import com.medisys.model.ReportRow;
import com.medisys.model.ReportSummary;
import com.medisys.model.SavedReport;
import com.medisys.model.User;
import com.medisys.util.TextUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All business rules of Reports and Analytics.
 *
 * Reports (admin)
 *  - a report covers a period: a preset (last 7 / 30 / 90 days, this month,
 *    last month, this year) or custom dates
 *  - custom dates must be real dates, "from" not after "to", not in the future,
 *    and at most MAX_DAYS days long
 *  - sales, prescriptions, deliveries and customers are counted for the period;
 *    the inventory part always shows the stock as it is now
 *  - every table can be downloaded as a CSV file (opens in Excel)
 *
 * Saved reports (admin)
 *  - "Save this report" stores the headline numbers of the period with a
 *    title (3-100 characters) and optional notes (up to 500), so months can
 *    be compared later
 *  - the numbers of a saved report are never changed; only the title and
 *    notes can be edited; a saved report can be deleted
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
public class ReportService {

    public static final int MAX_DAYS = 366;
    public static final int TOP_MEDICINES = 10;
    public static final int TOP_CUSTOMERS = 5;
    public static final int EXPIRY_WARNING_DAYS = 60;
    public static final int TITLE_MIN = 3;
    public static final int TITLE_MAX = 100;
    public static final int NOTES_MAX = 500;
    public static final String DEFAULT_PRESET = "30";

    /** Preset key -> label, in the order they are shown. */
    public static final Map<String, String> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("7", "Last 7 days");
        PRESETS.put("30", "Last 30 days");
        PRESETS.put("90", "Last 90 days");
        PRESETS.put("month", "This month");
        PRESETS.put("lastmonth", "Last month");
        PRESETS.put("year", "This year");
    }

    /** The CSV downloads: type -> file name part. */
    public static final Map<String, String> EXPORTS = new LinkedHashMap<>();

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

    private final ReportDAO reportDAO = new ReportDAOImpl();
    private final SavedReportDAO savedReportDAO = new SavedReportDAOImpl();

    // ============================================================= period

    /**
     * Works out the period from the page's parameters.
     * Custom dates (from / to) win over a preset.
     *
     * @throws ValidationException when the custom dates are not acceptable
     */
    public ReportPeriod resolvePeriod(String presetText, String fromText, String toText) throws ValidationException {
        LocalDate today = LocalDate.now();
        if (!TextUtil.isBlank(fromText) || !TextUtil.isBlank(toText)) {
            LocalDate from = TextUtil.parseDate(fromText);
            LocalDate to = TextUtil.parseDate(toText);
            if (from == null || to == null) {
                throw new ValidationException("Please choose both a start date and an end date.");
            }
            if (from.isAfter(to)) {
                throw new ValidationException("The start date must be on or before the end date.");
            }
            if (to.isAfter(today)) {
                throw new ValidationException("The end date can't be in the future.");
            }
            if (from.isBefore(LocalDate.of(2020, 1, 1))) {
                throw new ValidationException("Reports start from 1 January 2020.");
            }
            ReportPeriod custom = new ReportPeriod(from, to, "custom");
            if (custom.getDays() > MAX_DAYS) {
                throw new ValidationException("A report can cover at most " + MAX_DAYS + " days.");
            }
            return matchPreset(custom);
        }
        return preset(PRESETS.containsKey(presetText) ? presetText : DEFAULT_PRESET);
    }

    /** The period of a preset, ending today. */
    public ReportPeriod preset(String key) {
        LocalDate today = LocalDate.now();
        switch (key) {
            case "7":
                return new ReportPeriod(today.minusDays(6), today, key);
            case "90":
                return new ReportPeriod(today.minusDays(89), today, key);
            case "month":
                return new ReportPeriod(today.withDayOfMonth(1), today, key);
            case "lastmonth": {
                LocalDate first = today.withDayOfMonth(1).minusMonths(1);
                return new ReportPeriod(first, first.plusMonths(1).minusDays(1), key);
            }
            case "year":
                return new ReportPeriod(today.withDayOfYear(1), today, key);
            default:
                return new ReportPeriod(today.minusDays(29), today, "30");
        }
    }

    /** Custom dates that happen to equal a preset are shown as that preset (highlights its button). */
    private ReportPeriod matchPreset(ReportPeriod custom) {
        for (String key : PRESETS.keySet()) {
            ReportPeriod p = preset(key);
            if (p.getFrom().equals(custom.getFrom()) && p.getTo().equals(custom.getTo())) {
                return p;
            }
        }
        return custom;
    }

    // ============================================================= report

    /** Everything on the Reports page for one period. */
    public Report buildReport(ReportPeriod period) throws SQLException {
        Report report = new Report();
        report.setPeriod(period);
        report.setSummary(reportDAO.summary(period));
        report.setDailySales(fillMissingDays(period, reportDAO.dailySales(period)));
        report.setTopMedicines(reportDAO.topMedicines(period, TOP_MEDICINES));
        report.setCategories(reportDAO.salesByCategory(period));
        report.setTopCustomers(reportDAO.topCustomers(period, TOP_CUSTOMERS));
        report.setPrescriptionStatuses(reportDAO.prescriptionStatuses(period));
        report.setRiders(reportDAO.riderPerformance(period));
        report.setLowStock(reportDAO.lowStock());
        report.setExpiringSoon(reportDAO.expiringSoon(EXPIRY_WARNING_DAYS));
        report.setStockValue(reportDAO.stockValue());
        report.setOutOfStockCount(reportDAO.outOfStockCount());
        return report;
    }

    /** Only the headline numbers of a period (e.g. to compare with a saved report). */
    public ReportSummary getSummary(ReportPeriod period) throws SQLException {
        return reportDAO.summary(period);
    }

    /**
     * The database only returns days that had sales. The chart needs every
     * day of the period, so the missing ones are added with 0.
     */
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

    // ============================================================ export

    /**
     * One report table as CSV text (Excel opens it).
     *
     * @param type one of the EXPORTS keys
     */
    public String exportCsv(String type, ReportPeriod period) throws SQLException, ValidationException {
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
        return csv.toString();
    }

    /** "medisys-top-medicines-2026-08-19-to-2026-09-17.csv" */
    public String exportFileName(String type, ReportPeriod period) {
        return "medisys-" + EXPORTS.get(type) + "-" + period.getFrom() + "-to-" + period.getTo() + ".csv";
    }

    /** Adds one CSV line. Values are quoted, and quotes inside are doubled. */
    private static void line(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvValue(values[i]));
        }
        csv.append("\r\n");
    }

    /**
     * Makes a value safe for a CSV file. A value starting with = + - @ would
     * be run as a formula by Excel ("CSV injection"), so it gets a ' in front.
     * A number like -5 does not start a formula problem, so numbers are kept.
     */
    static String csvValue(String value) {
        String v = value == null ? "" : value;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !v.matches("-?\\d+(\\.\\d+)?")) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static String percent(Double value) {
        return value == null ? "" : String.format("%.1f", value);
    }

    // ===================================================== saved reports

    /** Saves the headline numbers of the period with a title and notes. Returns the new id. */
    public int saveReport(User admin, ReportPeriod period, String titleText, String notesText)
            throws SQLException, ValidationException {
        String title = checkTitle(titleText);
        String notes = checkNotes(notesText);

        ReportSummary s = reportDAO.summary(period);
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
        return savedReportDAO.create(r);
    }

    public List<SavedReport> getSavedReports() throws SQLException {
        return savedReportDAO.findAll();
    }

    public SavedReport getSavedReport(int id) throws SQLException {
        return savedReportDAO.findById(id);
    }

    public String updateSavedReport(int id, String titleText, String notesText)
            throws SQLException, ValidationException {
        String title = checkTitle(titleText);
        String notes = checkNotes(notesText);
        if (!savedReportDAO.update(id, title, notes)) {
            throw new ValidationException("That saved report no longer exists.");
        }
        return "The saved report was updated.";
    }

    public String deleteSavedReport(int id) throws SQLException, ValidationException {
        SavedReport r = savedReportDAO.findById(id);
        if (r == null || !savedReportDAO.delete(id)) {
            throw new ValidationException("That saved report no longer exists.");
        }
        return "\"" + r.getTitle() + "\" was deleted.";
    }

    private String checkTitle(String text) throws ValidationException {
        String title = TextUtil.clean(text).replaceAll("\\s+", " ");
        if (title.length() < TITLE_MIN || title.length() > TITLE_MAX) {
            throw new ValidationException("Please give the report a title (" + TITLE_MIN + " to " + TITLE_MAX
                    + " characters), e.g. \"September 2026 sales\".");
        }
        return title;
    }

    private String checkNotes(String text) throws ValidationException {
        String notes = TextUtil.clean(text);
        if (notes.length() > NOTES_MAX) {
            throw new ValidationException("The notes can have at most " + NOTES_MAX + " characters.");
        }
        return notes.isEmpty() ? null : notes;
    }

    private static BigDecimal toDecimal(Double percent) {
        return percent == null ? null : BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP);
    }
}
