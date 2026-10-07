<%--
    Customer's list of uploaded prescriptions, with the right action for each.
    Filled by MyPrescriptionsServlet (/prescriptions).

    Module : 05 - Prescription Upload and Verification
    Owner  : Perera D. A. A. N. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Prescription" %>
<%@ page import="com.medisys.model.PrescriptionStatus" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "My Prescriptions"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Prescription> prescriptions = (List<Prescription>) request.getAttribute("prescriptions");
    Integer highlight = (Integer) request.getAttribute("highlight");
%>
<%!
    /** CSS class of one progress step: "done", "current", "stopped" or "". */
    private String stepClass(boolean done, boolean current, boolean stopped) {
        return done ? "done" : current ? "current" : stopped ? "stopped" : "";
    }
%>

<div class="title-row">
    <div>
        <h1>My Prescriptions</h1>
        <p class="subtitle">Upload a prescription, our pharmacist lists the medicines, then you pay.</p>
    </div>
    <a class="btn" href="<%= ctx %>/prescriptions/upload">+ Upload prescription</a>
</div>

<% if (prescriptions.isEmpty()) { %>
    <div class="card empty-state">
        <h2>No prescriptions yet</h2>
        <p>Upload a photo or PDF of your prescription. Our pharmacist will list the medicines for you.</p>
        <a class="btn" href="<%= ctx %>/prescriptions/upload">Upload a prescription</a>
    </div>
<% } %>

<div class="rx-list">
<% for (Prescription p : prescriptions) {
       PrescriptionStatus status = p.getStatus();
       boolean expired = p.isExpired();
       boolean paid = p.isPaid();
       boolean approved = status == PrescriptionStatus.APPROVED;
       String viewUrl = ctx + "/prescriptions/view?id=" + p.getId();
%>
    <article class="card rx-card <%= Integer.valueOf(p.getId()).equals(highlight) ? "highlight" : "" %>"
             id="rx-<%= p.getId() %>">
        <div class="rx-head">
            <a class="file-chip small" href="<%= ctx %>/prescriptions/file?id=<%= p.getId() %>"
               target="_blank" rel="noopener" title="Open the file you uploaded">
                <span class="file-type"><%= p.isPdf() ? "PDF" : "IMG" %></span>
            </a>
            <div class="rx-title">
                <h2><a href="<%= viewUrl %>">Prescription <%= p.getReference() %></a></h2>
                <span class="meta">Uploaded <%= TextUtil.dateTime(p.getUploadedAt()) %>
                    &middot; <%= TextUtil.html(p.getOriginalFileName()) %></span>
            </div>
            <div class="rx-badges">
                <% if (paid) { %>
                    <span class="badge badge-paid">Paid</span>
                <% } else { %>
                    <span class="badge <%= status.getCssClass() %>"><%= status.getLabel() %></span>
                <% } %>
                <% if (expired) { %><span class="badge badge-expired">Expired</span><% } %>
            </div>
        </div>

        <ol class="rx-steps" aria-label="Progress">
            <li class="done">Uploaded</li>
            <li class="<%= stepClass(status != PrescriptionStatus.PENDING, status == PrescriptionStatus.PENDING, false) %>">
                Pharmacist check</li>
            <li class="<%= stepClass(approved, p.needsCorrection(), status == PrescriptionStatus.REJECTED) %>">
                <%= status == PrescriptionStatus.REJECTED ? "Rejected"
                        : p.needsCorrection() ? "New copy needed" : "Medicines listed" %></li>
            <li class="<%= stepClass(paid, approved && !paid && !expired, false) %>">Paid</li>
        </ol>

        <div class="rx-status-text">
            <% if (paid) { %>
                <p>Paid <strong><%= TextUtil.money(p.getAmountPaid()) %></strong> on <%= TextUtil.dateTime(p.getPaidAt()) %>
                    &middot; <%= TextUtil.html(p.getMedicineNames()) %></p>
            <% } else if (expired) { %>
                <p>This prescription is older than <%= Prescription.EXPIRY_DAYS %> days and can no longer be used.
                    Please upload a new one.</p>
            <% } else if (approved) { %>
                <p><strong><%= p.getItems().size() %> medicine<%= p.getItems().size() == 1 ? "" : "s" %></strong>:
                    <%= TextUtil.html(p.getMedicineNames()) %></p>
                <p>Total <strong><%= TextUtil.money(p.getTotal()) %></strong> &middot; pay before
                    <%= TextUtil.date(p.getExpiresAt().toLocalDate()) %></p>
            <% } else if (status == PrescriptionStatus.PENDING) { %>
                <p><% if (p.getCorrectionCount() > 0) { %>Thanks for the new copy. <% } %>
                    Our pharmacist is checking it. You will get a notification when your medicines are listed.</p>
            <% } %>
            <% if (p.getPharmacistNote() != null && status != PrescriptionStatus.PENDING) { %>
                <div class="note-box">
                    <span class="note-title">Note from our pharmacist</span>
                    <p><%= TextUtil.html(p.getPharmacistNote()) %></p>
                </div>
            <% } %>
        </div>

        <div class="actions">
            <% if (approved && !paid && !expired) { %>
                <a class="btn" href="<%= viewUrl %>">View medicines &amp; pay</a>
            <% } else if (paid) { %>
                <a class="btn" href="<%= viewUrl %>">View receipt</a>
            <% } else if (p.needsCorrection() && !expired) { %>
                <a class="btn correction" href="<%= ctx %>/prescriptions/correct?id=<%= p.getId() %>">Send corrected copy</a>
            <% } else if (expired || status == PrescriptionStatus.REJECTED) { %>
                <a class="btn" href="<%= ctx %>/prescriptions/upload">Upload a new prescription</a>
            <% } %>
            <% if (!(approved || paid)) { %>
                <a class="btn plain" href="<%= viewUrl %>">Details</a>
            <% } %>
            <a class="btn plain" href="<%= ctx %>/prescriptions/file?id=<%= p.getId() %>" target="_blank" rel="noopener">
                View file</a>
            <% if (p.isDeletable()) { %>
                <form method="post" action="<%= ctx %>/prescriptions/delete"
                      data-confirm="Delete prescription <%= p.getReference() %>? This cannot be undone.">
                    <input type="hidden" name="id" value="<%= p.getId() %>">
                    <button class="btn plain danger-text" type="submit">Delete</button>
                </form>
            <% } %>
        </div>
    </article>
<% } %>
</div>

<%@ include file="../common/footer.jspf" %>
