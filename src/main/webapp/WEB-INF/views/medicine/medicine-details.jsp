<%--
    Details of one medicine for customers.
    Filled by MedicineDetailsServlet (/medicines/view?id=).

    Module : 03 - Medicine Catalog and Inventory
    Owner  : Divisekara A. W. D. M. D. M. B.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Medicine" %>
<%
    Medicine m = (Medicine) request.getAttribute("medicine");
    String pageTitle = m.getDisplayName();
%>
<%@ include file="../common/header.jspf" %>

<p><a href="<%= ctx %>/medicines">&larr; Back to medicines</a></p>

<div class="card">
    <span class="category-name"><%= TextUtil.html(m.getCategoryName()) %></span>
    <h1><%= TextUtil.html(m.getDisplayName()) %></h1>
    <p class="subtitle"><%= TextUtil.html(m.getDosageForm()) %>
        <% if (m.getManufacturer() != null) { %> by <%= TextUtil.html(m.getManufacturer()) %><% } %>
    </p>

    <div class="price big"><%= TextUtil.money(m.getPrice()) %></div>
    <p>
        <span class="badge <%= m.getStockCssClass() %>"><%= m.getStockLabel() %></span>
        <% if (m.isRequiresPrescription()) { %>
            <span class="badge badge-rx">Prescription required</span>
        <% } %>
    </p>

    <% if (m.isRequiresPrescription()) { %>
        <div class="message info">
            This is a prescription-only medicine. When you add it to your cart you will
            be asked to upload your prescription. A senior pharmacist checks it before
            the order is released.
        </div>
    <% } %>

    <% if (m.getDescription() != null) { %>
        <h2>About this medicine</h2>
        <p class="description"><%= TextUtil.html(m.getDescription()) %></p>
    <% } %>

    <dl class="info">
        <dt>Category</dt><dd><%= TextUtil.html(m.getCategoryName()) %></dd>
        <dt>Dosage form</dt><dd><%= TextUtil.html(m.getDosageForm()) %></dd>
        <dt>Strength</dt><dd><%= m.getStrength() == null ? "-" : TextUtil.html(m.getStrength()) %></dd>
        <dt>Manufacturer</dt><dd><%= m.getManufacturer() == null ? "-" : TextUtil.html(m.getManufacturer()) %></dd>
        <dt>Expiry date</dt><dd><%= TextUtil.date(m.getExpiryDate()) %></dd>
    </dl>

    <div class="actions">
        <% if (m.isAvailable()) { %>
            <%-- Handled by module 01 (Shopping Cart). --%>
            <form method="post" action="<%= ctx %>/cart/add" class="add-to-cart">
                <input type="hidden" name="medicineId" value="<%= m.getId() %>">
                <label for="quantity">Quantity</label>
                <input type="number" id="quantity" name="quantity" value="1" min="1"
                       max="<%= m.getStockQuantity() %>" required>
                <button class="btn" type="submit">Add to Cart</button>
            </form>
            <%-- Handled by module 01 (Wishlist). --%>
            <form method="post" action="<%= ctx %>/wishlist/action">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="medicineId" value="<%= m.getId() %>">
                <button class="btn plain" type="submit">Add to Wishlist</button>
            </form>
        <% } else { %>
            <p class="subtitle">This medicine is out of stock at the moment.</p>
        <% } %>
    </div>
</div>

<%@ include file="../common/footer.jspf" %>
