<%--
    Medicine catalog - what customers browse.
    Filled by CatalogServlet (/medicines).

    Module : 03 - Medicine Catalog and Inventory (browsing)
             01 - Shopping Cart and Wishlist (cart / heart buttons)
    Owner  : Divisekara A. W. D. M. D. M. B.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Category" %>
<%@ page import="com.medisys.model.Medicine" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<% String pageTitle = "Medicines"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Medicine> medicines = (List<Medicine>) request.getAttribute("medicines");
    @SuppressWarnings("unchecked")
    List<Category> categories = (List<Category>) request.getAttribute("categories");
    @SuppressWarnings("unchecked")
    Map<Integer, Integer> cartQuantities = (Map<Integer, Integer>) request.getAttribute("cartQuantities");
    @SuppressWarnings("unchecked")
    Set<Integer> wishlistIds = (Set<Integer>) request.getAttribute("wishlistIds");
    String keyword = (String) request.getAttribute("keyword");
    Integer selectedCategory = (Integer) request.getAttribute("selectedCategory");
    String sort = (String) request.getAttribute("sort");

    // Guests see the shop buttons too (they are asked to log in when they press one).
    boolean shopper = currentUser == null || currentUser.isCustomer();

    // Keeps the search text and sort when a category pill is clicked.
    String keep = "";
    if (!keyword.isEmpty()) {
        keep += "&q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    }
    if (!sort.isEmpty()) {
        keep += "&sort=" + URLEncoder.encode(sort, StandardCharsets.UTF_8);
    }
%>

<section class="card catalog-head">
    <h1>Medicines</h1>
    <p class="subtitle">Order genuine medicines from our pharmacy. Items marked
        <span class="badge badge-rx">Rx</span> need a prescription that our pharmacist checks first.</p>

    <form class="search-bar" method="get" action="<%= ctx %>/medicines" role="search">
        <% if (selectedCategory != null) { %>
            <input type="hidden" name="category" value="<%= selectedCategory %>">
        <% } %>
        <input type="search" name="q" value="<%= TextUtil.html(keyword) %>"
               placeholder="Search by medicine or manufacturer" maxlength="100" aria-label="Search medicines">
        <select name="sort" data-autosubmit aria-label="Sort by">
            <option value="">Name A-Z</option>
            <option value="price-asc" <%= "price-asc".equals(sort) ? "selected" : "" %>>Price: low to high</option>
            <option value="price-desc" <%= "price-desc".equals(sort) ? "selected" : "" %>>Price: high to low</option>
        </select>
        <button class="btn" type="submit">Search</button>
    </form>

    <nav class="filters" aria-label="Categories">
        <a class="<%= selectedCategory == null ? "active" : "" %>"
           href="<%= ctx %>/medicines?<%= keep.isEmpty() ? "" : keep.substring(1) %>">All</a>
        <% for (Category c : categories) { %>
            <a class="<%= Integer.valueOf(c.getId()).equals(selectedCategory) ? "active" : "" %>"
               href="<%= ctx %>/medicines?category=<%= c.getId() %><%= keep %>"><%= TextUtil.html(c.getName()) %></a>
        <% } %>
    </nav>
</section>

<div class="result-row">
    <p class="result-count" aria-live="polite">
        <%= medicines.size() %> medicine<%= medicines.size() == 1 ? "" : "s" %>
        <% if (!keyword.isEmpty()) { %> for "<strong><%= TextUtil.html(keyword) %></strong>"<% } %>
    </p>
    <% if (!keyword.isEmpty() || selectedCategory != null || !sort.isEmpty()) { %>
        <a href="<%= ctx %>/medicines">Clear filters</a>
    <% } %>
</div>

<% if (medicines.isEmpty()) { %>
    <div class="card empty-state">
        <h2>No medicines found</h2>
        <p>Try a different spelling, or browse all categories.</p>
        <a class="btn" href="<%= ctx %>/medicines">Show all medicines</a>
    </div>
<% } else { %>
    <div class="catalog-grid">
        <% for (Medicine m : medicines) {
               int inCart = cartQuantities.getOrDefault(m.getId(), 0);
               boolean isSaved = wishlistIds.contains(m.getId());
               String detailsUrl = ctx + "/medicines/view?id=" + m.getId();
        %>
        <article class="medicine-card">
            <div class="card-media">
                <a class="<%= m.getThumbCssClass() %>" href="<%= detailsUrl %>" tabindex="-1" aria-hidden="true">
                    <%= m.getThumbText() %>
                </a>
                <% if (shopper) { %>
                    <%@ include file="../common/heart-button.jspf" %>
                <% } %>
            </div>

            <div class="medicine-card-top">
                <span class="category-name"><%= TextUtil.html(m.getCategoryName()) %></span>
                <% if (m.isRequiresPrescription()) { %>
                    <span class="badge badge-rx" title="Prescription required">Rx</span>
                <% } %>
            </div>
            <h2><a href="<%= detailsUrl %>"><%= TextUtil.html(m.getDisplayName()) %></a></h2>
            <p class="meta"><%= TextUtil.html(m.getDosageForm()) %>
                <% if (m.getManufacturer() != null) { %> &middot; <%= TextUtil.html(m.getManufacturer()) %><% } %>
            </p>

            <div class="price-row">
                <span class="price"><%= TextUtil.money(m.getPrice()) %></span>
                <span class="badge <%= m.getStockCssClass() %>"><%= m.getStockLabel() %></span>
            </div>

            <div class="card-actions">
                <% if (!shopper) { %>
                    <a class="btn plain small block" href="<%= detailsUrl %>">View details</a>
                <% } else if (!m.isAvailable()) { %>
                    <button class="btn small block" type="button" disabled>Out of stock</button>
                <% } else if (m.isRequiresPrescription()) { %>
                    <a class="btn small block rx" href="<%= detailsUrl %>#prescription">Prescription needed</a>
                <% } else { %>
                    <form method="post" action="<%= ctx %>/cart/add" data-ajax="cart-add">
                        <input type="hidden" name="medicineId" value="<%= m.getId() %>">
                        <input type="hidden" name="quantity" value="1">
                        <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                        <button class="btn small block" type="submit">Add to Cart</button>
                    </form>
                <% } %>
                <p class="in-cart <%= inCart == 0 ? "hidden" : "" %>" data-in-cart="<%= m.getId() %>">
                    <a href="<%= ctx %>/cart"><span><%= inCart %></span> in your cart</a>
                </p>
            </div>
        </article>
        <% } %>
    </div>
<% } %>

<script src="<%= ctx %>/js/medicine.js" defer></script>
<script src="<%= ctx %>/js/cart.js" defer></script>
<%@ include file="../common/footer.jspf" %>
