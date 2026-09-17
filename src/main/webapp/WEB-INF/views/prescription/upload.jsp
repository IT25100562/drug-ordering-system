<%--
    Customer uploads a prescription: only the file and an optional note.
    Filled by UploadPrescriptionServlet (/prescriptions/upload).

    Module : 05 - Prescription Upload and Verification
    Owner  : Perera D. A. A. N. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Prescription" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "Upload Prescription"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
    String note = (String) request.getAttribute("note");
    int maxFileMb = (Integer) request.getAttribute("maxFileBytes") / (1024 * 1024);
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/prescriptions">My Prescriptions</a> <span aria-hidden="true">/</span>
    <span aria-current="page">Upload</span>
</nav>

<div class="upload-layout">
    <section class="card">
        <h1>Upload your prescription</h1>
        <p class="subtitle">Just send us a photo or PDF of the prescription your doctor gave you.
            Our senior pharmacist reads it and lists the medicines and how to use them.</p>

        <% if (errors != null && !errors.isEmpty()) { %>
            <div class="message error" role="alert">
                <ul><% for (String e : errors) { %><li><%= TextUtil.html(e) %></li><% } %></ul>
                <p class="meta">For your safety, please choose the file again.</p>
            </div>
        <% } %>

        <form method="post" action="<%= ctx %>/prescriptions/upload" enctype="multipart/form-data"
              data-upload-form novalidate>

            <%@ include file="file-field.jspf" %>

            <div class="field">
                <label for="note">Note for the pharmacist <span class="meta">(optional)</span></label>
                <textarea id="note" name="note" rows="3" maxlength="500" data-counter="note-count"
                          placeholder="e.g. I only need the medicines for my blood pressure"><%= TextUtil.html(note) %></textarea>
                <div class="hint" id="note-count"></div>
            </div>

            <label class="confirm-check">
                <input type="checkbox" name="confirm" value="yes" required>
                I confirm this prescription was issued to me by a registered doctor.
            </label>

            <div class="actions">
                <button class="btn" type="submit" data-busy-text="Uploading...">Upload prescription</button>
                <a class="btn plain" href="<%= ctx %>/prescriptions">Cancel</a>
            </div>
        </form>
    </section>

    <aside class="card help-card">
        <h2>How it works</h2>
        <ol class="steps">
            <li><strong>Upload</strong> a photo or PDF of your prescription.</li>
            <li><strong>Our pharmacist checks it</strong> and lists each medicine with how to use it.</li>
            <li><strong>You get a notification</strong>, see the medicines and the total, and pay.</li>
            <li><strong>We deliver</strong> the medicines to your door.</li>
        </ol>
        <h2>For a quick approval</h2>
        <ul class="tips">
            <li>The whole page is visible and in focus</li>
            <li>Doctor's name, registration number and signature can be read</li>
            <li>Your name and the date are on it</li>
            <li>It was issued in the last <%= Prescription.EXPIRY_DAYS %> days</li>
        </ul>
        <p class="meta">Your prescription is only seen by you and our pharmacists.</p>
    </aside>
</div>

<script src="<%= ctx %>/js/prescription.js" defer></script>
<%@ include file="../common/footer.jspf" %>
