<%--
    Saved reports: snapshots of past periods, newest first.
    Filled by SavedReportsServlet (/admin/reports/saved).

    Module : 04 - Reports and Analytics
    Owner  : Kaweesha P. M. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.SavedReport" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "Saved reports"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<SavedReport> savedReports = (List<SavedReport>) request.getAttribute("savedReports");
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/admin/reports">Reports</a> <span aria-hidden="true">/</span>
    <span aria-current="page">Saved reports</span>
</nav>

<div class="title-row">
    <div>
        <h1>Saved reports</h1>
        <p class="subtitle">Numbers kept as they were on the day each report was saved, so periods can be compared.</p>
    </div>
    <a class="btn" href="<%= ctx %>/admin/reports">+ New report</a>
</div>

<section class="card">
    <% if (savedReports.isEmpty()) { %>
        <div class="empty">No saved reports yet. Open a report and use <strong>Save this report</strong>.</div>
    <% } else { %>
    <div class="table-wrap">
    <table class="report-table">
        <tr>
            <th>Report</th>
            <th>Period</th>
            <th class="num">Revenue</th>
            <th class="num">Orders</th>
            <th class="num">Prescriptions</th>
            <th class="num">On time</th>
            <th><span class="sr-only">Actions</span></th>
        </tr>
        <% for (SavedReport r : savedReports) { %>
        <tr>
            <td><a href="<%= ctx %>/admin/reports/saved/view?id=<%= r.getId() %>"><strong><%= TextUtil.html(r.getTitle()) %></strong></a>
                <br><span class="meta"><%= r.getReference() %> &middot; saved by <%= TextUtil.html(r.getCreatedByName()) %>,
                    <%= TextUtil.timeAgo(r.getCreatedAt()) %></span></td>
            <td class="nowrap"><%= r.getPeriod().getLabel() %></td>
            <td class="num nowrap"><%= TextUtil.money(r.getRevenue()) %></td>
            <td class="num"><%= r.getOrderCount() %></td>
            <td class="num"><%= r.getPrescriptionCount() %></td>
            <td class="num"><%= r.getOnTimeRate() == null ? "-" : r.getOnTimeRate().setScale(0, java.math.RoundingMode.HALF_UP) + "%" %></td>
            <td class="row-actions">
                <a class="btn small plain" href="<%= ctx %>/admin/reports/saved/view?id=<%= r.getId() %>">Open</a>
                <form method="post" action="<%= ctx %>/admin/reports/saved"
                      data-confirm="Delete the saved report &quot;<%= TextUtil.html(r.getTitle()) %>&quot;?">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="<%= r.getId() %>">
                    <button class="btn small plain danger-text" type="submit">Delete</button>
                </form>
            </td>
        </tr>
        <% } %>
    </table>
    </div>
    <% } %>
</section>

<%@ include file="../common/footer.jspf" %>
