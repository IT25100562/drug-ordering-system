<%--
    The customer's wishlist (saved medicines).
    Filled by WishlistServlet (/wishlist).

    Module : 01 - Shopping Cart and Wishlist
    Owner  : Amadini G. G. A.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Medicine" %>
<%@ page import="com.medisys.model.WishlistItem" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<% String pageTitle = "My Wishlist"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<WishlistItem> items = (List<WishlistItem>) request.getAttribute("items");
    @SuppressWarnings("unchecked")
    Set<Integer> needsPrescription = (Set<Integer>) request.getAttribute("needsPrescription");
    @SuppressWarnings("unchecked")
    Map<Integer, Integer> cartQuantities = (Map<Integer, Integer>) request.getAttribute("cartQuantities");
%>

<div class="title-row">
    <div>
        <h1>My Wishlist</h1>
        <p class="subtitle" data-wishlist-summary><%= items.isEmpty() ? "Nothing saved yet."
                : items.size() + " saved medicine" + (items.size() == 1 ? "" : "s") %></p>
    </div>
    <a class="btn plain" href="<%= ctx %>/cart">Go to cart &rarr;</a>
</div>

<div class="card empty-state <%= items.isEmpty() ? "" : "hidden" %>" data-wishlist-empty>
    <h2>Your wishlist is empty</h2>
    <p>Tap the heart on any medicine to save it for later.</p>
    <a class="btn" href="<%= ctx %>/medicines">Browse medicines</a>
</div>

<div class="catalog-grid">
    <% for (WishlistItem item : items) {
           Medicine m = item.getMedicine();
           boolean onSale = !m.isDiscontinued() && !m.isExpired();
           int inCart = cartQuantities.getOrDefault(m.getId(), 0);
           String detailsUrl = ctx + "/medicines/view?id=" + m.getId();
    %>
    <article class="medicine-card" data-wish="<%= m.getId() %>">
        <div class="card-media">
            <a class="<%= m.getThumbCssClass() %>" href="<%= onSale ? detailsUrl : "#" %>" tabindex="-1"
               aria-hidden="true"><%= m.getThumbText() %></a>
            <% if (item.getAddedAt() != null) { %>
                <span class="meta">Saved <%= TextUtil.date(item.getAddedAt().toLocalDate()) %></span>
            <% } %>
        </div>
        <div class="medicine-card-top">
            <span class="category-name"><%= TextUtil.html(m.getCategoryName()) %></span>
            <% if (m.isRequiresPrescription()) { %><span class="badge badge-rx">Rx</span><% } %>
        </div>
        <h2><% if (onSale) { %><a href="<%= detailsUrl %>"><%= TextUtil.html(m.getDisplayName()) %></a><%
               } else { %><%= TextUtil.html(m.getDisplayName()) %><% } %></h2>
        <p class="meta"><%= TextUtil.html(m.getDosageForm()) %></p>

        <div class="price-row">
            <span class="price"><%= TextUtil.money(m.getPrice()) %></span>
            <% if (!onSale) { %>
                <span class="badge badge-expired">No longer sold</span>
            <% } else { %>
                <span class="badge <%= m.getStockCssClass() %>"><%= m.getStockLabel() %></span>
            <% } %>
        </div>

        <div class="card-actions">
            <% if (!m.isAvailable()) { %>
                <button class="btn small block" type="button" disabled>
                    <%= onSale ? "Out of stock" : "Unavailable" %></button>
            <% } else if (needsPrescription.contains(m.getId())) { %>
                <a class="btn small block rx" href="<%= detailsUrl %>#prescription">Prescription needed</a>
            <% } else { %>
                <form method="post" action="<%= ctx %>/wishlist/action" data-ajax="wishlist-move">
                    <input type="hidden" name="medicineId" value="<%= m.getId() %>">
                    <input type="hidden" name="action" value="move">
                    <button class="btn small block" type="submit">Move to cart</button>
                </form>
            <% } %>
            <form method="post" action="<%= ctx %>/wishlist/action" data-ajax="wishlist-remove">
                <input type="hidden" name="medicineId" value="<%= m.getId() %>">
                <input type="hidden" name="action" value="remove">
                <button class="btn small plain block" type="submit">Remove</button>
            </form>
            <% if (inCart > 0) { %>
                <p class="in-cart"><a href="<%= ctx %>/cart"><%= inCart %> already in your cart</a></p>
            <% } %>
        </div>
    </article>
    <% } %>
</div>

<script src="<%= ctx %>/js/cart.js" defer></script>
<%@ include file="../common/footer.jspf" %>
