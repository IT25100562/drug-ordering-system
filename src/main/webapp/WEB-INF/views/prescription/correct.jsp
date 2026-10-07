<%--
    Customer sends a corrected copy of a prescription.
    Filled by CorrectionUploadServlet (/prescriptions/correct?id=).

    Module : 05 - Prescription Upload and Verification
    Owner  : Perera D. A. A. N. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Prescription" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "Send Corrected Copy"; %>
<%@ include file="../common/header.jspf" %>
<%
    Prescription p = (Prescription) request.getAttribute("prescription");
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
    int maxFileMb = (Integer) request.getAttribute("maxFileBytes") / (1024 * 1024);
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/prescriptions">My Prescriptions</a> <span aria-hidden="true">/</span>
    <span aria-current="page">Send corrected copy</span>
</nav>

<div class="upload-layout">
    <section class="card">
        <h1>Send a corrected copy</h1>
        <p class="subtitle">Prescription <%= p.getReference() %> &middot; uploaded <%= TextUtil.dateTime(p.getUploadedAt()) %></p>

        <div class="note-box">
            <span class="note-title">Our pharmacist asked:</span>
            <p><%= TextUtil.html(p.getPharmacistNote()) %></p>
            <span class="meta">&mdash; <%= TextUtil.html(p.getReviewedByName()) %>,
                <%= TextUtil.dateTime(p.getReviewedAt()) %></span>
        </div>

        <% if (errors != null && !errors.isEmpty()) { %>
            <div class="message error" role="alert">
                <ul><% for (String e : errors) { %><li><%= TextUtil.html(e) %></li><% } %></ul>
            </div>
        <% } %>

        <form method="post" action="<%= ctx %>/prescriptions/correct" enctype="multipart/form-data"
              data-upload-form novalidate>
            <input type="hidden" name="id" value="<%= p.getId() %>">
            <%@ include file="file-field.jspf" %>
            <div class="actions">
                <button class="btn" type="submit" data-busy-text="Sending...">Send new copy</button>
                <a class="btn plain" href="<%= ctx %>/prescriptions">Cancel</a>
            </div>
        </form>
    </section>

    <aside class="card help-card">
        <h2>The copy you sent</h2>
        <a class="file-chip" href="<%= ctx %>/prescriptions/file?id=<%= p.getId() %>" target="_blank" rel="noopener">
            <span class="file-type"><%= p.isPdf() ? "PDF" : "IMG" %></span>
            <span><%= TextUtil.html(p.getOriginalFileName()) %><br>
                <span class="meta"><%= p.getFileSizeLabel() %> &middot; opens in a new tab</span></span>
        </a>
        <h2>Tips for a clear copy</h2>
        <ul class="tips">
            <li>Place the paper on a flat, dark surface</li>
            <li>Use daylight and avoid shadows and glare</li>
            <li>Hold the phone straight above the page</li>
            <li>Check the photo is sharp before sending</li>
        </ul>
    </aside>
</div>

<script src="<%= ctx %>/js/prescription.js" defer></script>
<%@ include file="../common/footer.jspf" %>
