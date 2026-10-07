<%--
    Admin add / edit medicine form.
    Filled by MedicineFormServlet (/admin/medicines/edit).

    Module : 03 - Medicine Catalog and Inventory
    Owner  : Divisekara A. W. D. M. D. M. B.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Category" %>
<%@ page import="com.medisys.model.Medicine" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
    int id = (Integer) request.getAttribute("id");
    boolean editing = id > 0;
    String pageTitle = editing ? "Edit Medicine" : "Add Medicine";
%>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    Map<String, String> form = (Map<String, String>) request.getAttribute("form");
    @SuppressWarnings("unchecked")
    List<Category> categories = (List<Category>) request.getAttribute("categories");
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
    Medicine saved = (Medicine) request.getAttribute("medicine");
%>
<%!
    // Returns the escaped form value, or "" when the field is empty.
    private String val(Map<String, String> form, String field) {
        return com.medisys.util.TextUtil.html(form.get(field));
    }
%>

<p><a href="<%= ctx %>/admin/medicines">&larr; Back to inventory</a></p>

<div class="card">
    <h1><%= pageTitle %></h1>
    <% if (editing && saved != null) { %>
        <p class="subtitle">ID <%= id %> &middot; last updated <%= TextUtil.dateTime(saved.getUpdatedAt()) %>
            <% if (saved.isDiscontinued()) { %> &middot; <span class="badge badge-expired">Discontinued</span><% } %>
        </p>
    <% } else { %>
        <p class="subtitle">Fields marked * are required.</p>
    <% } %>

    <form id="medicine-form" method="post" action="<%= ctx %>/admin/medicines/edit" novalidate>
        <% if (errors != null && !errors.isEmpty()) { %>
            <div class="message error js-errors">
                <ul>
                    <% for (String error : errors) { %><li><%= TextUtil.html(error) %></li><% } %>
                </ul>
            </div>
        <% } %>

        <input type="hidden" name="id" value="<%= id %>">

        <div class="row">
            <div class="field">
                <label for="name">Name *</label>
                <input type="text" id="name" name="name" value="<%= val(form, "name") %>"
                       maxlength="150" required>
            </div>
            <div class="field">
                <label for="strength">Strength</label>
                <input type="text" id="strength" name="strength" value="<%= val(form, "strength") %>"
                       maxlength="50" placeholder="e.g. 500 mg">
            </div>
        </div>

        <div class="row">
            <div class="field">
                <label for="categoryId">Category *</label>
                <select id="categoryId" name="categoryId" required>
                    <option value="">-- choose --</option>
                    <% for (Category c : categories) { %>
                        <option value="<%= c.getId() %>"
                                <%= String.valueOf(c.getId()).equals(form.get("categoryId")) ? "selected" : "" %>>
                            <%= TextUtil.html(c.getName()) %>
                        </option>
                    <% } %>
                </select>
                <div class="hint"><a href="<%= ctx %>/admin/categories">Manage categories</a></div>
            </div>
            <div class="field">
                <label for="dosageForm">Dosage form *</label>
                <select id="dosageForm" name="dosageForm" required>
                    <option value="">-- choose --</option>
                    <% for (String dosageForm : Medicine.DOSAGE_FORMS) { %>
                        <option <%= dosageForm.equals(form.get("dosageForm")) ? "selected" : "" %>><%= dosageForm %></option>
                    <% } %>
                </select>
            </div>
        </div>

        <div class="field">
            <label for="manufacturer">Manufacturer</label>
            <input type="text" id="manufacturer" name="manufacturer"
                   value="<%= val(form, "manufacturer") %>" maxlength="150">
        </div>

        <div class="field">
            <label for="description">Description</label>
            <textarea id="description" name="description" rows="4"
                      maxlength="2000"><%= val(form, "description") %></textarea>
            <div class="hint" id="description-count"></div>
        </div>

        <div class="row">
            <div class="field">
                <label for="price">Price (Rs.) *</label>
                <input type="number" id="price" name="price" value="<%= val(form, "price") %>"
                       min="0.01" max="1000000" step="0.01" required>
            </div>
            <div class="field">
                <label for="stockQuantity">Stock quantity *</label>
                <input type="number" id="stockQuantity" name="stockQuantity"
                       value="<%= val(form, "stockQuantity") %>" min="0" max="100000" step="1" required>
            </div>
            <div class="field">
                <label for="reorderLevel">Reorder level *</label>
                <input type="number" id="reorderLevel" name="reorderLevel"
                       value="<%= val(form, "reorderLevel") %>" min="0" max="10000" step="1" required>
                <div class="hint">Shown as "Low stock" at or below this.</div>
            </div>
        </div>

        <div class="row">
            <div class="field">
                <label for="expiryDate">Expiry date</label>
                <input type="date" id="expiryDate" name="expiryDate" value="<%= val(form, "expiryDate") %>">
            </div>
            <div class="field checkbox-field">
                <label>
                    <input type="checkbox" name="requiresPrescription"
                           <%= form.get("requiresPrescription") != null ? "checked" : "" %>>
                    Prescription required
                </label>
                <div class="hint">Customers must upload a prescription that a pharmacist approves.</div>
            </div>
        </div>

        <div class="actions">
            <button class="btn" type="submit"><%= editing ? "Save changes" : "Add medicine" %></button>
            <a class="btn plain" href="<%= ctx %>/admin/medicines">Cancel</a>
        </div>
    </form>
</div>

<script src="<%= ctx %>/js/medicine.js" defer></script>
<%@ include file="../common/footer.jspf" %>
