<%--
    Admin inventory: stock levels, search, filters, restock, discontinue / restore.
    Filled by InventoryServlet (/admin/medicines).

    Module : 03 - Medicine Catalog and Inventory
    Owner  : Divisekara A. W. D. M. D. M. B.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.dao.MedicineDAO" %>
<%@ page import="com.medisys.model.Category" %>
<%@ page import="com.medisys.model.InventorySummary" %>
<%@ page import="com.medisys.model.Medicine" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "Inventory"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Medicine> medicines = (List<Medicine>) request.getAttribute("medicines");
    @SuppressWarnings("unchecked")
    List<Category> categories = (List<Category>) request.getAttribute("categories");
    InventorySummary summary = (InventorySummary) request.getAttribute("summary");
    String keyword = (String) request.getAttribute("keyword");
    Integer selectedCategory = (Integer) request.getAttribute("selectedCategory");
    String filter = (String) request.getAttribute("filter");

    // The current search, so the action forms can come back to the same list.
    String currentQuery = "filter=" + filter;
    if (!keyword.isEmpty()) {
        currentQuery += "&q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    }
    if (selectedCategory != null) {
        currentQuery += "&category=" + selectedCategory;
    }

    // Filter buttons: {filter value, label, count}
    Object[][] filters = {
            {MedicineDAO.FILTER_ACTIVE, "All active", summary.getActiveCount()},
            {MedicineDAO.FILTER_LOW_STOCK, "Low stock", summary.getLowStockCount()},
            {MedicineDAO.FILTER_OUT_OF_STOCK, "Out of stock", summary.getOutOfStockCount()},
            {MedicineDAO.FILTER_EXPIRED, "Expired", summary.getExpiredCount()},
            {MedicineDAO.FILTER_DISCONTINUED, "Discontinued", summary.getDiscontinuedCount()}
    };
    boolean showingDiscontinued = MedicineDAO.FILTER_DISCONTINUED.equals(filter);
%>

<div class="card">
    <div class="title-row">
        <div>
            <h1>Inventory</h1>
            <p class="subtitle">Manage medicines, prices and stock levels.</p>
        </div>
        <div class="actions">
            <a class="btn plain" href="<%= ctx %>/admin/categories">Categories</a>
            <a class="btn" href="<%= ctx %>/admin/medicines/edit">+ Add medicine</a>
        </div>
    </div>

    <div class="stats">
        <div class="stat"><span><%= summary.getActiveCount() %></span>Active medicines</div>
        <div class="stat warn"><span><%= summary.getLowStockCount() %></span>Low stock</div>
        <div class="stat danger"><span><%= summary.getOutOfStockCount() %></span>Out of stock</div>
        <div class="stat muted"><span><%= summary.getExpiredCount() %></span>Expired</div>
    </div>

    <div class="filters">
        <% for (Object[] f : filters) {
               String value = (String) f[0];
        %>
            <a class="<%= value.equals(filter) ? "active" : "" %>"
               href="<%= ctx %>/admin/medicines?filter=<%= value %>"><%= f[1] %> (<%= f[2] %>)</a>
        <% } %>
    </div>

    <form class="search-bar" method="get" action="<%= ctx %>/admin/medicines">
        <input type="hidden" name="filter" value="<%= TextUtil.html(filter) %>">
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
    </form>

    <% if (medicines.isEmpty()) { %>
        <div class="empty">No medicines in this list.</div>
    <% } else { %>
    <div class="table-wrap">
    <table>
        <tr>
            <th>ID</th>
            <th>Medicine</th>
            <th>Category</th>
            <th>Price</th>
            <th>Stock</th>
            <th>Expiry</th>
            <th>Actions</th>
        </tr>
        <% for (Medicine m : medicines) { %>
        <tr>
            <td><%= m.getId() %></td>
            <td>
                <strong><%= TextUtil.html(m.getDisplayName()) %></strong><br>
                <span class="meta"><%= TextUtil.html(m.getDosageForm()) %>
                    <% if (m.getManufacturer() != null) { %> &middot; <%= TextUtil.html(m.getManufacturer()) %><% } %>
                </span>
                <% if (m.isRequiresPrescription()) { %><br><span class="badge badge-rx">Prescription</span><% } %>
            </td>
            <td><%= TextUtil.html(m.getCategoryName()) %></td>
            <td class="nowrap"><%= TextUtil.money(m.getPrice()) %></td>
            <td>
                <strong><%= m.getStockQuantity() %></strong>
                <span class="meta">/ reorder at <%= m.getReorderLevel() %></span><br>
                <span class="badge <%= m.getStockCssClass() %>"><%= m.getStockLabel() %></span>
            </td>
            <td class="nowrap">
                <%= TextUtil.date(m.getExpiryDate()) %>
                <% if (m.isExpired()) { %><br><span class="badge badge-expired">Expired</span><% } %>
            </td>
            <td class="row-actions">
                <% if (showingDiscontinued) { %>
                    <form method="post" action="<%= ctx %>/admin/medicines/discontinue">
                        <input type="hidden" name="id" value="<%= m.getId() %>">
                        <input type="hidden" name="action" value="restore">
                        <input type="hidden" name="returnQuery" value="<%= TextUtil.html(currentQuery) %>">
                        <button class="btn small approve" type="submit">Restore</button>
                    </form>
                <% } else { %>
                    <form method="post" action="<%= ctx %>/admin/medicines/restock" class="restock">
                        <input type="hidden" name="id" value="<%= m.getId() %>">
                        <input type="hidden" name="returnQuery" value="<%= TextUtil.html(currentQuery) %>">
                        <input type="number" name="quantity" min="1" max="10000" placeholder="Qty"
                               required aria-label="Quantity to add">
                        <button class="btn small" type="submit">+ Add</button>
                    </form>
                    <a class="btn small plain" href="<%= ctx %>/admin/medicines/edit?id=<%= m.getId() %>">Edit</a>
                    <form method="post" action="<%= ctx %>/admin/medicines/discontinue"
                          data-confirm="Discontinue <%= TextUtil.html(m.getDisplayName()) %>? It will be hidden from the catalog.">
                        <input type="hidden" name="id" value="<%= m.getId() %>">
                        <input type="hidden" name="action" value="discontinue">
                        <input type="hidden" name="returnQuery" value="<%= TextUtil.html(currentQuery) %>">
                        <button class="btn small reject" type="submit">Discontinue</button>
                    </form>
                <% } %>
            </td>
        </tr>
        <% } %>
    </table>
    </div>
    <% } %>
</div>

<script src="<%= ctx %>/js/medicine.js" defer></script>
<%@ include file="../common/footer.jspf" %>
