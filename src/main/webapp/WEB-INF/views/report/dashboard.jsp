<%--
    Reports and Analytics dashboard (admin).
    Sales, prescriptions, deliveries, customers and inventory for a period,
    with CSV downloads and "Save this report".
    Filled by ReportsServlet (/admin/reports).

    The charts are plain HTML + CSS bars (no chart library): each bar's width
    or height is its value as a percentage of the largest value. Every bar has
    a hover / keyboard tooltip, and every chart has a table view.

    Module : 04 - Reports and Analytics
    Owner  : Kaweesha P. M. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Report" %>
<%@ page import="com.medisys.model.ReportPeriod" %>
<%@ page import="com.medisys.model.ReportRow" %>
<%@ page import="com.medisys.model.ReportSummary" %>
<%@ page import="com.medisys.service.ReportService" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<% String pageTitle = "Reports & Analytics"; %>
<%@ include file="../common/header.jspf" %>
<%
    Report report = (Report) request.getAttribute("report");
    ReportPeriod period = report.getPeriod();
    ReportSummary s = report.getSummary();
    String exportBase = ctx + "/admin/reports/export?" + period.getQuery() + "&type=";

    // Sales chart scale: the largest day, and three x-axis labels (first, middle, last day).
    List<ReportRow> days = report.getDailySales();
    double maxDay = Report.maxAmount(days);
    int middle = days.size() / 2;
%>
<%!
    /** A percentage for the page, e.g. "83%", or "-" when there is nothing to divide. */
    private String pct(Double value) {
        return value == null ? "-" : Math.round(value) + "%";
    }

    /** CSS width/height of a bar: value as a percentage of max. Tiny values stay visible. */
    private String size(double value, double max) {
        if (value <= 0) {
            return "0";
        }
        return String.format(java.util.Locale.ROOT, "%.1f%%", Math.max(1.5, value * 100 / max));
    }

    /** "Rs. 12.4k" for chart axes, where full amounts would not fit. */
    private String shortMoney(double amount) {
        if (amount >= 1000) {
            return String.format(java.util.Locale.ROOT, "Rs. %.1fk", amount / 1000);
        }
        return String.format(java.util.Locale.ROOT, "Rs. %.0f", amount);
    }

    /** "2026-09-17" -> "17 Sep" */
    private String shortDay(String isoDate) {
        return java.time.LocalDate.parse(isoDate).format(
                java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH));
    }
%>

<div class="title-row">
    <div>
        <h1>Reports &amp; Analytics</h1>
        <p class="subtitle"><strong><%= period.getLabel() %></strong> &middot; <%= period.getDays() %> day<%= period.getDays() == 1 ? "" : "s" %></p>
    </div>
    <div class="actions no-print">
        <a class="btn plain" href="<%= ctx %>/admin/reports/saved">Saved reports</a>
        <button class="btn plain" type="button" onclick="window.print()">Print</button>
    </div>
</div>

<%-- -------------------------------------------------------------- period filter --%>
<section class="card report-filter no-print">
    <nav class="filters" aria-label="Report period">
        <% for (Map.Entry<String, String> preset : ReportService.PRESETS.entrySet()) {
               boolean active = preset.getKey().equals(period.getPreset());
        %>
            <a class="<%= active ? "active" : "" %>" href="<%= ctx %>/admin/reports?range=<%= preset.getKey() %>"
               <%= active ? "aria-current=\"page\"" : "" %>><%= preset.getValue() %></a>
        <% } %>
    </nav>
    <form class="date-range" method="get" action="<%= ctx %>/admin/reports">
        <label for="from">From</label>
        <input type="date" id="from" name="from" value="<%= period.getFrom() %>" max="<%= java.time.LocalDate.now() %>" required>
        <label for="to">To</label>
        <input type="date" id="to" name="to" value="<%= period.getTo() %>" max="<%= java.time.LocalDate.now() %>" required>
        <button class="btn small" type="submit">Show</button>
    </form>
    <details class="save-report">
        <summary>Save this report</summary>
        <form method="post" action="<%= ctx %>/admin/reports/saved">
            <input type="hidden" name="action" value="create">
            <input type="hidden" name="from" value="<%= period.getFrom() %>">
            <input type="hidden" name="to" value="<%= period.getTo() %>">
            <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
            <div class="field">
                <label for="title">Title *</label>
                <input type="text" id="title" name="title" minlength="<%= ReportService.TITLE_MIN %>"
                       maxlength="<%= ReportService.TITLE_MAX %>" required placeholder="e.g. September 2026 sales">
            </div>
            <div class="field">
                <label for="notes">Notes <span class="meta">(optional)</span></label>
                <textarea id="notes" name="notes" rows="2" maxlength="<%= ReportService.NOTES_MAX %>"
                          placeholder="e.g. Dengue season - Panadol sold out twice"></textarea>
            </div>
            <button class="btn small" type="submit">Save</button>
            <span class="meta">The headline numbers are kept as they are today.</span>
        </form>
    </details>
</section>

<%-- ------------------------------------------------------------------ headline --%>
<div class="kpi-grid">
    <div class="kpi">
        <span class="kpi-label">Revenue</span>
        <span class="kpi-value"><%= TextUtil.money(s.getRevenue()) %></span>
        <span class="meta"><%= s.getOrderCount() %> paid order<%= s.getOrderCount() == 1 ? "" : "s" %></span>
    </div>
    <div class="kpi">
        <span class="kpi-label">Average order</span>
        <span class="kpi-value"><%= TextUtil.money(s.getAverageOrder()) %></span>
        <span class="meta">incl. <%= TextUtil.money(s.getDeliveryFees()) %> delivery fees</span>
    </div>
    <div class="kpi">
        <span class="kpi-label">Customers who ordered</span>
        <span class="kpi-value"><%= s.getBuyingCustomers() %></span>
        <span class="meta"><%= s.getNewCustomers() %> new account<%= s.getNewCustomers() == 1 ? "" : "s" %></span>
    </div>
    <div class="kpi">
        <span class="kpi-label">Cancelled</span>
        <span class="kpi-value"><%= s.getCancelledCount() %></span>
        <span class="meta"><%= TextUtil.money(s.getRefundedAmount()) %> refunded</span>
    </div>
</div>
<p class="meta report-links no-print">Download:
    <a href="<%= exportBase %>summary">summary (CSV)</a>
</p>

<%-- ---------------------------------------------------------- sales over time --%>
<section class="card">
    <div class="section-head">
        <h2>Revenue per day</h2>
        <a class="no-print" href="<%= exportBase %>daily">Download CSV</a>
    </div>
    <% if (s.getOrderCount() == 0) { %>
        <p class="empty">No sales in this period.</p>
    <% } else { %>
    <div class="column-chart" role="img"
         aria-label="Revenue per day, <%= period.getLabel() %>. Highest day <%= TextUtil.money(java.math.BigDecimal.valueOf(maxDay)) %>. See the table below for every day.">
        <div class="y-axis" aria-hidden="true">
            <span><%= shortMoney(maxDay) %></span>
            <span><%= shortMoney(maxDay / 2) %></span>
            <span>Rs. 0</span>
        </div>
        <div class="plot">
            <% for (ReportRow day : days) {
                   String tip = shortDay(day.getLabel()) + ": " + TextUtil.money(day.getAmount())
                           + " (" + day.getCount() + " order" + (day.getCount() == 1 ? "" : "s") + ")";
            %>
                <span class="col" tabindex="0" data-tip="<%= TextUtil.html(tip) %>">
                    <span class="col-bar" style="height:<%= size(day.getAmount().doubleValue(), maxDay) %>"></span>
                </span>
            <% } %>
        </div>
        <div class="x-axis" aria-hidden="true">
            <span><%= shortDay(days.get(0).getLabel()) %></span>
            <% if (days.size() > 2) { %><span><%= shortDay(days.get(middle).getLabel()) %></span><% } %>
            <% if (days.size() > 1) { %><span><%= shortDay(days.get(days.size() - 1).getLabel()) %></span><% } %>
        </div>
    </div>
    <p class="source-split">
        <span><strong><%= TextUtil.money(s.getCartRevenue()) %></strong> from the shop (cart)</span>
        <span><strong><%= TextUtil.money(s.getPrescriptionRevenue()) %></strong> from prescriptions
            <span class="meta">(<%= pct(s.getPrescriptionRevenueShare()) %>)</span></span>
    </p>
    <details class="table-view">
        <summary>Show as table</summary>
        <div class="table-wrap">
        <table class="report-table">
            <tr><th>Date</th><th class="num">Orders</th><th class="num">Revenue</th></tr>
            <% for (ReportRow day : days) { if (day.getCount() == 0) { continue; } %>
                <tr><td><%= day.getLabel() %></td><td class="num"><%= day.getCount() %></td>
                    <td class="num"><%= TextUtil.money(day.getAmount()) %></td></tr>
            <% } %>
        </table>
        </div>
    </details>
    <% } %>
</section>

<%-- ---------------------------------------------- medicines and categories --%>
<div class="report-two">
    <section class="card">
        <div class="section-head">
            <h2>Top <%= ReportService.TOP_MEDICINES %> medicines</h2>
            <a class="no-print" href="<%= exportBase %>medicines">Download CSV</a>
        </div>
        <% if (report.getTopMedicines().isEmpty()) { %>
            <p class="empty">No sales in this period.</p>
        <% } else {
               double maxMed = Report.maxAmount(report.getTopMedicines());
        %>
        <ol class="hbar-list">
            <% for (ReportRow m : report.getTopMedicines()) { %>
                <li>
                    <span class="hbar-label"><%= TextUtil.html(m.getLabel()) %>
                        <span class="meta"><%= m.getCount() %> pack<%= m.getCount() == 1 ? "" : "s" %></span></span>
                    <span class="hbar-track">
                        <span class="hbar" tabindex="0" style="width:<%= size(m.getAmount().doubleValue(), maxMed) %>"
                              data-tip="<%= TextUtil.html(m.getLabel() + ": " + TextUtil.money(m.getAmount()) + ", " + m.getCount() + " packs, " + m.getDetail()) %>"></span>
                    </span>
                    <span class="hbar-value"><%= TextUtil.money(m.getAmount()) %></span>
                </li>
            <% } %>
        </ol>
        <% } %>
    </section>

    <section class="card">
        <div class="section-head">
            <h2>Sales by category</h2>
            <a class="no-print" href="<%= exportBase %>categories">Download CSV</a>
        </div>
        <% if (report.getCategories().isEmpty()) { %>
            <p class="empty">No sales in this period.</p>
        <% } else {
               double maxCat = Report.maxAmount(report.getCategories());
        %>
        <ol class="hbar-list">
            <% for (ReportRow c : report.getCategories()) { %>
                <li>
                    <span class="hbar-label"><%= TextUtil.html(c.getLabel()) %>
                        <span class="meta"><%= c.getCount() %> pack<%= c.getCount() == 1 ? "" : "s" %></span></span>
                    <span class="hbar-track">
                        <span class="hbar" tabindex="0" style="width:<%= size(c.getAmount().doubleValue(), maxCat) %>"
                              data-tip="<%= TextUtil.html(c.getLabel() + ": " + TextUtil.money(c.getAmount()) + ", " + c.getCount() + " packs") %>"></span>
                    </span>
                    <span class="hbar-value"><%= TextUtil.money(c.getAmount()) %></span>
                </li>
            <% } %>
        </ol>
        <% } %>
    </section>
</div>

<%-- ------------------------------------------------------------ prescriptions --%>
<section class="card">
    <div class="section-head">
        <h2>Prescriptions</h2>
        <a class="no-print" href="<%= exportBase %>prescriptions">Download CSV</a>
    </div>
    <div class="kpi-grid small">
        <div class="kpi"><span class="kpi-label">Uploaded</span><span class="kpi-value"><%= s.getPrescriptionCount() %></span></div>
        <div class="kpi"><span class="kpi-label">Approval rate</span><span class="kpi-value"><%= pct(s.getApprovalRate()) %></span>
            <span class="meta">approved out of approved + rejected</span></div>
        <div class="kpi"><span class="kpi-label">Average review time</span>
            <span class="kpi-value"><%= s.getAverageReviewHours() == null ? "-" : String.format(java.util.Locale.ROOT, "%.1f h", s.getAverageReviewHours()) %></span>
            <span class="meta">upload to pharmacist's decision</span></div>
        <div class="kpi"><span class="kpi-label">Paid after approval</span><span class="kpi-value"><%= pct(s.getPrescriptionPayRate()) %></span>
            <span class="meta"><%= s.getPaidPrescriptionCount() %> of <%= s.getApprovedCount() %> approved</span></div>
    </div>
    <% double maxRx = Report.maxCount(report.getPrescriptionStatuses()); %>
    <ol class="hbar-list compact">
        <% for (ReportRow st : report.getPrescriptionStatuses()) { %>
            <li>
                <span class="hbar-label"><%= TextUtil.html(st.getDetail()) %></span>
                <span class="hbar-track">
                    <span class="hbar" tabindex="0" style="width:<%= size(st.getCount(), maxRx) %>"
                          data-tip="<%= TextUtil.html(st.getDetail() + ": " + st.getCount()) %>"></span>
                </span>
                <span class="hbar-value"><%= st.getCount() %></span>
            </li>
        <% } %>
    </ol>
</section>

<%-- --------------------------------------------------------------- deliveries --%>
<section class="card">
    <div class="section-head">
        <h2>Deliveries</h2>
        <a class="no-print" href="<%= exportBase %>riders">Download CSV</a>
    </div>
    <div class="kpi-grid small">
        <div class="kpi"><span class="kpi-label">Delivered</span><span class="kpi-value"><%= s.getDeliveredCount() %></span></div>
        <div class="kpi"><span class="kpi-label">On time</span><span class="kpi-value"><%= pct(s.getOnTimeRate()) %></span>
            <span class="meta">on or before the expected day</span></div>
        <div class="kpi"><span class="kpi-label">Failed attempts</span><span class="kpi-value"><%= s.getFailedAttempts() %></span>
            <span class="meta">nobody at home, wrong address ...</span></div>
    </div>
    <div class="table-wrap">
    <table class="report-table">
        <tr><th>Rider</th><th class="num">Delivered</th><th>On time</th><th class="num">Failed attempts</th></tr>
        <% for (ReportRow r : report.getRiders()) {
               Double onTime = r.getExtraPercent();
        %>
            <tr>
                <td><%= TextUtil.html(r.getLabel()) %></td>
                <td class="num"><%= r.getCount() %></td>
                <td>
                    <span class="meter" role="img" aria-label="<%= pct(onTime) %> on time">
                        <span style="width:<%= onTime == null ? "0" : size(onTime, 100) %>"></span>
                    </span>
                    <%= pct(onTime) %>
                </td>
                <td class="num"><%= TextUtil.html(r.getDetail()) %></td>
            </tr>
        <% } %>
    </table>
    </div>
</section>

<%-- ---------------------------------------------------------------- customers --%>
<section class="card">
    <div class="section-head">
        <h2>Top customers</h2>
        <a class="no-print" href="<%= exportBase %>customers">Download CSV</a>
    </div>
    <% if (report.getTopCustomers().isEmpty()) { %>
        <p class="empty">No orders in this period.</p>
    <% } else { %>
    <div class="table-wrap">
    <table class="report-table">
        <tr><th>#</th><th>Customer</th><th class="num">Orders</th><th class="num">Spent</th></tr>
        <% int rank = 1; for (ReportRow c : report.getTopCustomers()) { %>
            <tr><td><%= rank++ %></td><td><%= TextUtil.html(c.getLabel()) %></td>
                <td class="num"><%= c.getCount() %></td><td class="num"><%= TextUtil.money(c.getAmount()) %></td></tr>
        <% } %>
    </table>
    </div>
    <% } %>
</section>

<%-- ---------------------------------------------------------------- inventory --%>
<section class="card">
    <div class="section-head">
        <h2>Inventory today</h2>
        <span class="meta">not limited to the period</span>
    </div>
    <div class="kpi-grid small">
        <div class="kpi"><span class="kpi-label">Stock value</span><span class="kpi-value"><%= TextUtil.money(report.getStockValue()) %></span>
            <span class="meta">selling price x quantity</span></div>
        <div class="kpi"><span class="kpi-label">Low stock</span><span class="kpi-value"><%= report.getLowStock().size() %></span>
            <span class="meta">at or below reorder level</span></div>
        <div class="kpi"><span class="kpi-label">Out of stock</span><span class="kpi-value"><%= report.getOutOfStockCount() %></span></div>
        <div class="kpi"><span class="kpi-label">Expiring soon</span><span class="kpi-value"><%= report.getExpiringSoon().size() %></span>
            <span class="meta">within <%= ReportService.EXPIRY_WARNING_DAYS %> days (or expired)</span></div>
    </div>
    <div class="report-two">
        <div>
            <div class="section-head">
                <h3>Low stock - reorder</h3>
                <a class="no-print" href="<%= exportBase %>lowstock">Download CSV</a>
            </div>
            <% if (report.getLowStock().isEmpty()) { %>
                <p class="meta">Everything is above its reorder level.</p>
            <% } else { %>
            <table class="report-table">
                <tr><th>Medicine</th><th class="num">In stock</th><th class="num">Reorder at</th></tr>
                <% for (ReportRow m : report.getLowStock()) { %>
                    <tr><td><%= TextUtil.html(m.getLabel()) %> <span class="meta"><%= TextUtil.html(m.getDetail()) %></span></td>
                        <td class="num"><% if (m.getCount() == 0) { %><span class="badge badge-rejected">Out</span><% } else { %><%= m.getCount() %><% } %></td>
                        <td class="num"><%= m.getExtra() %></td></tr>
                <% } %>
            </table>
            <% } %>
        </div>
        <div>
            <div class="section-head">
                <h3>Expiring soon</h3>
                <a class="no-print" href="<%= exportBase %>expiring">Download CSV</a>
            </div>
            <% if (report.getExpiringSoon().isEmpty()) { %>
                <p class="meta">Nothing expires in the next <%= ReportService.EXPIRY_WARNING_DAYS %> days.</p>
            <% } else { %>
            <table class="report-table">
                <tr><th>Medicine</th><th>Expires</th><th class="num">In stock</th></tr>
                <% for (ReportRow m : report.getExpiringSoon()) {
                       boolean expired = java.time.LocalDate.parse(m.getDetail()).isBefore(java.time.LocalDate.now());
                %>
                    <tr><td><%= TextUtil.html(m.getLabel()) %></td>
                        <td><%= m.getDetail() %> <% if (expired) { %><span class="badge badge-rejected">Expired</span><% } %></td>
                        <td class="num"><%= m.getCount() %></td></tr>
                <% } %>
            </table>
            <% } %>
        </div>
    </div>
    <p class="meta no-print"><a href="<%= ctx %>/admin/medicines">Open the inventory</a> to restock.</p>
</section>

<%@ include file="../common/footer.jspf" %>
