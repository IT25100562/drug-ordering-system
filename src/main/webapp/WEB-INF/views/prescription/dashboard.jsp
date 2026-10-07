<%--
    Verification Dashboard for the senior pharmacist.
    Filled by PharmacistDashboardServlet (/pharmacist/dashboard?status=).

    Module : 05 - Prescription Upload and Verification
    Owner  : Perera D. A. A. N. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Prescription" %>
<%@ page import="com.medisys.model.PrescriptionStatus" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<% String pageTitle = "Verification Dashboard"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Prescription> prescriptions = (List<Prescription>) request.getAttribute("prescriptions");
    @SuppressWarnings("unchecked")
    Map<String, Integer> counts = (Map<String, Integer>) request.getAttribute("counts");
    String filter = (String) request.getAttribute("filter");

    // Filter tabs: {value, label}
    String[][] tabs = {
            {"PENDING", "Waiting for verification"},
            {"CORRECTION_REQUESTED", "Correction requested"},
            {"APPROVED", "Approved, not paid"},
            {"PAID", "Paid"},
            {"REJECTED", "Rejected"},
            {"EXPIRED", "Expired"},
            {"ALL", "All"}
    };
    String emptyText = "PENDING".equals(filter) ? "All caught up - no prescriptions are waiting."
            : "No prescriptions in this list.";
%>

<div class="title-row">
    <div>
        <h1>Verification Dashboard</h1>
        <p class="subtitle">Check each prescription for authenticity, then list the medicines and
            how to use them. The customer pays after your approval.</p>
    </div>
</div>

<div class="stats">
    <a class="stat warn" href="<%= ctx %>/pharmacist/dashboard?status=PENDING">
        <span><%= counts.get("PENDING") %></span>Waiting for verification</a>
    <a class="stat" href="<%= ctx %>/pharmacist/dashboard?status=CORRECTION_REQUESTED">
        <span><%= counts.get("CORRECTION_REQUESTED") %></span>Waiting for the customer</a>
    <a class="stat" href="<%= ctx %>/pharmacist/dashboard?status=APPROVED">
        <span><%= counts.get("APPROVED") %></span>Waiting for payment</a>
    <a class="stat" href="<%= ctx %>/pharmacist/dashboard?status=PAID">
        <span><%= counts.get("PAID") %></span>Paid</a>
    <a class="stat danger" href="<%= ctx %>/pharmacist/dashboard?status=EXPIRED">
        <span><%= counts.get("EXPIRED") %></span>Expired (can be deleted)</a>
</div>

<section class="card">
    <nav class="filters" aria-label="Filter prescriptions">
        <% for (String[] tab : tabs) { %>
            <a class="<%= tab[0].equals(filter) ? "active" : "" %>"
               href="<%= ctx %>/pharmacist/dashboard?status=<%= tab[0] %>"
               <%= tab[0].equals(filter) ? "aria-current=\"page\"" : "" %>>
                <%= tab[1] %> (<%= counts.get(tab[0]) %>)</a>
        <% } %>
    </nav>

    <% if (prescriptions.isEmpty()) { %>
        <div class="empty"><%= emptyText %></div>
    <% } else { %>
    <div class="table-wrap">
    <table class="rx-table">
        <tr>
            <th>Reference</th>
            <th>Customer</th>
            <th>Medicines</th>
            <th>Uploaded</th>
            <th>Status</th>
            <th><span class="sr-only">Actions</span></th>
        </tr>
        <% for (Prescription p : prescriptions) { %>
        <tr class="<%= p.isExpired() ? "row-muted" : "" %> <%= p.isCustomerFlagged() ? "row-flagged" : "" %>">
            <td>
                <strong><%= p.getReference() %></strong>
                <br><span class="meta"><%= p.isPdf() ? "PDF" : "Image" %> &middot; <%= p.getFileSizeLabel() %></span>
            </td>
            <td>
                <div class="person">
                    <%= avatar(ctx, p.getUserId(), p.isCustomerHasPhoto(), p.getCustomerName(), "avatar small") %>
                    <span><%= TextUtil.html(p.getCustomerName()) %>
                        <% if (p.isCustomerFlagged()) { %><span class="badge flag-badge" title="Flagged by a pharmacist">&#9873; Flagged</span><% } %>
                        <br><span class="meta"><%= TextUtil.html(p.getCustomerEmail()) %></span></span>
                </div>
            </td>
            <td>
                <% if (p.getItems().isEmpty()) { %>
                    <span class="meta">Not listed yet</span>
                <% } else { %>
                    <%= TextUtil.html(p.getMedicineNames()) %>
                    <br><span class="meta"><%= TextUtil.money(p.getTotal()) %></span>
                <% } %>
            </td>
            <td class="nowrap">
                <span title="<%= TextUtil.dateTime(p.getUploadedAt()) %>"><%= TextUtil.timeAgo(p.getUploadedAt()) %></span>
                <% if (p.getCorrectionCount() > 0) { %>
                    <br><span class="meta">corrected copy #<%= p.getCorrectionCount() %></span>
                <% } %>
                <% if (p.isExpired()) { %><br><span class="badge badge-expired">Expired</span><% } %>
            </td>
            <td>
                <% if (p.isPaid()) { %>
                    <span class="badge badge-paid">Paid</span>
                <% } else { %>
                    <span class="badge <%= p.getStatus().getCssClass() %>"><%= p.getStatus().getLabel() %></span>
                <% } %>
            </td>
            <td class="row-actions">
                <a class="btn small <%= p.isAwaitingReview() && !p.isExpired() ? "" : "plain" %>"
                   href="<%= ctx %>/pharmacist/review?id=<%= p.getId() %>">
                    <%= p.isAwaitingReview() && !p.isExpired() ? "Review" : "Open" %></a>
                <% if (p.isDeletable()) { %>
                    <form method="post" action="<%= ctx %>/pharmacist/delete"
                          data-confirm="Delete <%= p.getReference() %> and its file? The customer will be notified.">
                        <input type="hidden" name="id" value="<%= p.getId() %>">
                        <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                        <button class="btn small <%= p.isExpired() ? "reject" : "plain" %>" type="submit">Delete</button>
                    </form>
                <% } %>
            </td>
        </tr>
        <% } %>
    </table>
    </div>
    <% } %>
</section>

<%@ include file="../common/footer.jspf" %>
