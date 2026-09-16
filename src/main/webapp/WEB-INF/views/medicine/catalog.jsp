<%--
    Medicine catalog - what customers browse.
    Filled by CatalogServlet (/medicines).

    Module : 03 - Medicine Catalog and Inventory
    Owner  : Divisekara A. W. D. M. D. M. B.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Category" %>
<%@ page import="com.medisys.model.Medicine" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "Medicines"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Medicine> medicines = (List<Medicine>) request.getAttribute("medicines");
    @SuppressWarnings("unchecked")
    List<Category> categories = (List<Category>) request.getAttribute("categories");
    String keyword = (String) request.getAttribute("keyword");
    Integer selectedCategory = (Integer) request.getAttribute("selectedCategory");
%>

<div class="card">
    <h1>Medicines</h1>
    <p class="subtitle">Browse our pharmacy. Medicines marked
        <span class="badge badge-rx">Prescription</span> need a prescription
        checked by our pharmacist before they can be ordered.</p>

    <form class="search-bar" method="get" action="<%= ctx %>/medicines">
        <input type="text" name="q" value="<%= TextUtil.html(keyword) %>"
               placeholder="Search by name or manufacturer" maxlength="100">
        <select name="category" data-autosubmit>
            <option value="">All categories</option>
            <% for (Category c : categories) { %>
                <option value="<%= c.getId() %>"
                        <%= Integer.valueOf(c.getId()).equals(selectedCategory) ? "selected" : "" %>>
                    <%= TextUtil.html(c.getName()) %>
                </option>
            <% } %>
        </select>
        <button class="btn" type="submit">Search</button>
        <% if (!keyword.isEmpty() || selectedCategory != null) { %>
            <a class="btn plain" href="<%= ctx %>/medicines">Clear</a>
        <% } %>
    </form>
</div>

<% if (medicines.isEmpty()) { %>
    <div class="card empty">No medicines match your search.</div>
<% } else { %>
    <p class="result-count"><%= medicines.size() %> medicine<%= medicines.size() == 1 ? "" : "s" %> found</p>
    <div class="catalog-grid">
        <% for (Medicine m : medicines) { %>
        <div class="medicine-card">
            <div class="medicine-card-top">
                <span class="category-name"><%= TextUtil.html(m.getCategoryName()) %></span>
                <% if (m.isRequiresPrescription()) { %>
                    <span class="badge badge-rx">Prescription</span>
                <% } %>
            </div>
            <h2>
                <a href="<%= ctx %>/medicines/view?id=<%= m.getId() %>"><%= TextUtil.html(m.getDisplayName()) %></a>
            </h2>
            <p class="meta"><%= TextUtil.html(m.getDosageForm()) %>
                <% if (m.getManufacturer() != null) { %> &middot; <%= TextUtil.html(m.getManufacturer()) %><% } %>
            </p>
            <div class="price"><%= TextUtil.money(m.getPrice()) %></div>
            <span class="badge <%= m.getStockCssClass() %>"><%= m.getStockLabel() %></span>

            <div class="actions">
                <a class="btn plain small" href="<%= ctx %>/medicines/view?id=<%= m.getId() %>">Details</a>
                <% if (m.isAvailable()) { %>
                    <%-- Handled by module 01 (Shopping Cart). --%>
                    <form method="post" action="<%= ctx %>/cart/add">
                        <input type="hidden" name="medicineId" value="<%= m.getId() %>">
                        <input type="hidden" name="quantity" value="1">
                        <button class="btn small" type="submit">Add to Cart</button>
                    </form>
                <% } %>
            </div>
        </div>
        <% } %>
    </div>
<% } %>

<script src="<%= ctx %>/js/medicine.js" defer></script>
<%@ include file="../common/footer.jspf" %>
