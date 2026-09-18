<%--
    One saved report: the numbers as saved, next to the same period today
    (a late cancellation or delivery can change them), plus edit / delete.
    Filled by SavedReportServlet (/admin/reports/saved/view?id=).

    Module : 04 - Reports and Analytics
    Owner  : Kaweesha P. M. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.ReportSummary" %>
<%@ page import="com.medisys.model.SavedReport" %>
<%@ page import="com.medisys.service.ReportService" %>
<%@ page import="java.math.BigDecimal" %>
<% String pageTitle = "Saved report"; %>
<%@ include file="../common/header.jspf" %>
<%
    SavedReport r = (SavedReport) request.getAttribute("saved");
    ReportSummary live = (ReportSummary) request.getAttribute("live");
%>
<%!
    private String pct(BigDecimal value) {
        return value == null ? "-" : value.setScale(0, java.math.RoundingMode.HALF_UP) + "%";
    }

    private String pct(Double value) {
        return value == null ? "-" : Math.round(value) + "%";
    }

    /** "same", or the change from the saved value to today's, e.g. "+Rs. 300.00" / "-2". */
    private String change(BigDecimal saved, BigDecimal now, boolean money) {
        int cmp = now.compareTo(saved);
        if (cmp == 0) {
            return "<span class=\"meta\">same</span>";
        }
        BigDecimal diff = now.subtract(saved);
        String text = money ? TextUtil.money(diff.abs()) : diff.abs().stripTrailingZeros().toPlainString();
        return "<span class=\"change\">" + (cmp > 0 ? "+" : "-") + text + "</span>";
    }
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/admin/reports">Reports</a> <span aria-hidden="true">/</span>
    <a href="<%= ctx %>/admin/reports/saved">Saved reports</a> <span aria-hidden="true">/</span>
    <span aria-current="page"><%= r.getReference() %></span>
</nav>

<div class="title-row">
    <div>
        <h1><%= TextUtil.html(r.getTitle()) %></h1>
        <p class="subtitle"><strong><%= r.getPeriod().getLabel() %></strong> &middot; saved by
            <%= TextUtil.html(r.getCreatedByName()) %> on <%= TextUtil.dateTime(r.getCreatedAt()) %></p>
    </div>
    <div class="actions no-print">
        <a class="btn" href="<%= ctx %>/admin/reports?<%= r.getPeriod().getQuery() %>">Open the full report</a>
        <button class="btn plain" type="button" onclick="window.print()">Print</button>
    </div>
</div>

<% if (r.getNotes() != null) { %>
    <div class="note-box"><span class="note-title">Notes</span><p><%= TextUtil.html(r.getNotes()) %></p></div>
<% } %>

<div class="view-layout">
    <section class="card">
        <h2>Numbers</h2>
        <div class="table-wrap">
        <table class="report-table">
            <tr><th>Measure</th><th class="num">When saved</th><th class="num">Today</th><th class="num">Change</th></tr>
            <tr><td>Revenue</td><td class="num"><%= TextUtil.money(r.getRevenue()) %></td>
                <td class="num"><%= TextUtil.money(live.getRevenue()) %></td>
                <td class="num"><%= change(r.getRevenue(), live.getRevenue(), true) %></td></tr>
            <tr><td>Paid orders</td><td class="num"><%= r.getOrderCount() %></td><td class="num"><%= live.getOrderCount() %></td>
                <td class="num"><%= change(BigDecimal.valueOf(r.getOrderCount()), BigDecimal.valueOf(live.getOrderCount()), false) %></td></tr>
            <tr><td>Average order</td><td class="num"><%= TextUtil.money(r.getAverageOrder()) %></td>
                <td class="num"><%= TextUtil.money(live.getAverageOrder()) %></td>
                <td class="num"><%= change(r.getAverageOrder(), live.getAverageOrder(), true) %></td></tr>
            <tr><td>Cancelled orders</td><td class="num"><%= r.getCancelledCount() %></td><td class="num"><%= live.getCancelledCount() %></td>
                <td class="num"><%= change(BigDecimal.valueOf(r.getCancelledCount()), BigDecimal.valueOf(live.getCancelledCount()), false) %></td></tr>
            <tr><td>Refunded</td><td class="num"><%= TextUtil.money(r.getRefundedAmount()) %></td>
                <td class="num"><%= TextUtil.money(live.getRefundedAmount()) %></td>
                <td class="num"><%= change(r.getRefundedAmount(), live.getRefundedAmount(), true) %></td></tr>
            <tr><td>Prescriptions uploaded</td><td class="num"><%= r.getPrescriptionCount() %></td><td class="num"><%= live.getPrescriptionCount() %></td>
                <td class="num"><%= change(BigDecimal.valueOf(r.getPrescriptionCount()), BigDecimal.valueOf(live.getPrescriptionCount()), false) %></td></tr>
            <tr><td>Approval rate</td><td class="num"><%= pct(r.getApprovalRate()) %></td><td class="num"><%= pct(live.getApprovalRate()) %></td><td></td></tr>
            <tr><td>Deliveries completed</td><td class="num"><%= r.getDeliveredCount() %></td><td class="num"><%= live.getDeliveredCount() %></td>
                <td class="num"><%= change(BigDecimal.valueOf(r.getDeliveredCount()), BigDecimal.valueOf(live.getDeliveredCount()), false) %></td></tr>
            <tr><td>On time</td><td class="num"><%= pct(r.getOnTimeRate()) %></td><td class="num"><%= pct(live.getOnTimeRate()) %></td><td></td></tr>
            <tr><td>New customers</td><td class="num"><%= r.getNewCustomers() %></td><td class="num"><%= live.getNewCustomers() %></td>
                <td class="num"><%= change(BigDecimal.valueOf(r.getNewCustomers()), BigDecimal.valueOf(live.getNewCustomers()), false) %></td></tr>
        </table>
        </div>
        <p class="meta">"Today" recalculates the same period now. It differs when, for example, an
            order of that period was cancelled or delivered after the report was saved.</p>
    </section>

    <aside class="no-print">
        <section class="card">
            <h2>Edit</h2>
            <form method="post" action="<%= ctx %>/admin/reports/saved">
                <input type="hidden" name="action" value="update">
                <input type="hidden" name="id" value="<%= r.getId() %>">
                <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                <div class="field">
                    <label for="title">Title *</label>
                    <input type="text" id="title" name="title" value="<%= TextUtil.html(r.getTitle()) %>"
                           minlength="<%= ReportService.TITLE_MIN %>" maxlength="<%= ReportService.TITLE_MAX %>" required>
                </div>
                <div class="field">
                    <label for="notes">Notes</label>
                    <textarea id="notes" name="notes" rows="4" maxlength="<%= ReportService.NOTES_MAX %>"><%= TextUtil.html(r.getNotes()) %></textarea>
                </div>
                <button class="btn block" type="submit">Save changes</button>
            </form>
            <% if (r.getUpdatedAt() != null && r.getCreatedAt() != null && r.getUpdatedAt().isAfter(r.getCreatedAt().plusSeconds(1))) { %>
                <p class="meta">Last edited <%= TextUtil.dateTime(r.getUpdatedAt()) %></p>
            <% } %>
        </section>
        <form method="post" action="<%= ctx %>/admin/reports/saved"
              data-confirm="Delete this saved report? The live report is not affected.">
            <input type="hidden" name="action" value="delete">
            <input type="hidden" name="id" value="<%= r.getId() %>">
            <button class="link-button danger" type="submit">Delete this saved report</button>
        </form>
    </aside>
</div>

<%@ include file="../common/footer.jspf" %>
