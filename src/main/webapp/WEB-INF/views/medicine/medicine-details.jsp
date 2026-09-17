<%--
    Details of one medicine for customers.
    Filled by MedicineDetailsServlet (/medicines/view?id=).

    Module : 03 - Medicine Catalog and Inventory (details)
             01 - Shopping Cart and Wishlist (buy box)
    Owner  : Divisekara A. W. D. M. D. M. B.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Medicine" %>
<%@ page import="java.util.List" %>
<%
    Medicine medicine = (Medicine) request.getAttribute("medicine");
    String pageTitle = medicine.getDisplayName();
%>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Medicine> related = (List<Medicine>) request.getAttribute("related");
    int maxQuantity = (Integer) request.getAttribute("maxQuantity");
    int inCart = (Integer) request.getAttribute("inCart");
    boolean needsPrescription = (Boolean) request.getAttribute("needsPrescription");
    boolean shopper = currentUser == null || currentUser.isCustomer();
    int canStillAdd = Math.max(0, maxQuantity - inCart);

    // The heart fragment expects "m" and "isSaved".
    Medicine m = medicine;
    boolean isSaved = (Boolean) request.getAttribute("saved");
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/medicines">Medicines</a> <span aria-hidden="true">/</span>
    <a href="<%= ctx %>/medicines?category=<%= m.getCategoryId() %>"><%= TextUtil.html(m.getCategoryName()) %></a>
    <span aria-hidden="true">/</span>
    <span aria-current="page"><%= TextUtil.html(m.getDisplayName()) %></span>
</nav>

<div class="details-grid">
    <%-- ------------------------------------------------ left: information --%>
    <section class="card">
        <div class="details-head">
            <div class="<%= m.getThumbCssClass() %> large" aria-hidden="true"><%= m.getThumbText() %></div>
            <div>
                <span class="category-name"><%= TextUtil.html(m.getCategoryName()) %></span>
                <h1><%= TextUtil.html(m.getDisplayName()) %></h1>
                <p class="subtitle"><%= TextUtil.html(m.getDosageForm()) %>
                    <% if (m.getManufacturer() != null) { %> by <%= TextUtil.html(m.getManufacturer()) %><% } %>
                </p>
                <p>
                    <span class="badge <%= m.getStockCssClass() %>"><%= m.getStockLabel() %></span>
                    <% if (m.isRequiresPrescription()) { %>
                        <span class="badge badge-rx">Prescription required</span>
                    <% } %>
                </p>
            </div>
        </div>

        <h2>About this medicine</h2>
        <p class="description"><%= m.getDescription() == null
                ? "No description has been added yet." : TextUtil.html(m.getDescription()) %></p>

        <h2>Product information</h2>
        <dl class="info">
            <dt>Category</dt><dd><%= TextUtil.html(m.getCategoryName()) %></dd>
            <dt>Dosage form</dt><dd><%= TextUtil.html(m.getDosageForm()) %></dd>
            <dt>Strength</dt><dd><%= m.getStrength() == null ? "-" : TextUtil.html(m.getStrength()) %></dd>
            <dt>Manufacturer</dt><dd><%= m.getManufacturer() == null ? "-" : TextUtil.html(m.getManufacturer()) %></dd>
            <dt>Expiry date</dt><dd><%= TextUtil.date(m.getExpiryDate()) %></dd>
            <dt>Prescription</dt><dd><%= m.isRequiresPrescription() ? "Required" : "Not required" %></dd>
        </dl>

        <p class="safety-note">Always read the label and follow the directions of your doctor or
            pharmacist. Keep medicines out of reach of children.</p>
    </section>

    <%-- ------------------------------------------------ right: buy box --%>
    <aside class="card buy-box" aria-label="Buy">
        <div class="price big"><%= TextUtil.money(m.getPrice()) %></div>
        <p class="meta">per pack &middot; <%= m.isOutOfStock() ? "currently unavailable"
                : m.getStockQuantity() <= m.getReorderLevel() ? "only " + m.getStockQuantity() + " left"
                : "in stock" %></p>

        <% if (!shopper) { %>
            <p class="meta">You are logged in as <%= TextUtil.html(currentUser.getRole().getLabel()) %>.
                Only customers can buy medicines.</p>

        <% } else if (!m.isAvailable()) { %>
            <div class="message info">This medicine is out of stock right now. Save it to your
                wishlist and check back later.</div>
            <div class="buy-row">
                <%@ include file="../common/heart-button.jspf" %>
                <span class="meta"><%= isSaved ? "Saved in your wishlist" : "Save for later" %></span>
            </div>

        <% } else if (needsPrescription) { %>
            <div class="rx-panel" id="prescription">
                <h2>Prescription required</h2>
                <ol>
                    <li>Upload a clear photo or PDF of your prescription.</li>
                    <li>Our senior pharmacist checks it (usually within a few hours).</li>
                    <li>Once approved, you can add this medicine to your cart.</li>
                </ol>
                <a class="btn block rx" href="<%= ctx %>/prescriptions/upload?medicineId=<%= m.getId() %>">
                    Upload prescription</a>
            </div>
            <div class="buy-row">
                <%@ include file="../common/heart-button.jspf" %>
                <span class="meta">Save for later</span>
            </div>

        <% } else { %>
            <% if (canStillAdd == 0) { %>
                <div class="message info">You already have the most you can buy
                    (<%= maxQuantity %>) in your cart.</div>
                <a class="btn block" href="<%= ctx %>/cart">Go to cart</a>
            <% } else { %>
                <form method="post" action="<%= ctx %>/cart/add" data-ajax="cart-add" class="buy-form"
                      data-limit="<%= maxQuantity %>">
                    <input type="hidden" name="medicineId" value="<%= m.getId() %>">
                    <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                    <label for="quantity">Quantity</label>
                    <div class="stepper" data-stepper>
                        <button type="button" data-step="-1" aria-label="Decrease quantity">&minus;</button>
                        <input type="number" id="quantity" name="quantity" value="1" min="1"
                               max="<%= canStillAdd %>" step="1" inputmode="numeric" required>
                        <button type="button" data-step="1" aria-label="Increase quantity">+</button>
                    </div>
                    <p class="hint">You can add up to <%= canStillAdd %> more.</p>
                    <button class="btn block" type="submit">Add to Cart</button>
                </form>
                <%-- Outside the form above: HTML does not allow a form inside a form. --%>
                <div class="buy-row">
                    <%@ include file="../common/heart-button.jspf" %>
                    <span class="meta">Save to wishlist</span>
                </div>
            <% } %>
        <% } %>

        <p class="in-cart <%= inCart == 0 ? "hidden" : "" %>" data-in-cart="<%= m.getId() %>">
            <a href="<%= ctx %>/cart"><span><%= inCart %></span> in your cart &rarr;</a>
        </p>

        <ul class="perks">
            <li>Checked by a registered pharmacist</li>
            <li>Island-wide delivery</li>
            <li>Stored at the right temperature</li>
        </ul>
    </aside>
</div>

<% if (!related.isEmpty()) { %>
<section class="related">
    <h2>More in <%= TextUtil.html(m.getCategoryName()) %></h2>
    <div class="catalog-grid compact">
        <% for (Medicine r : related) { %>
            <a class="medicine-card link-card" href="<%= ctx %>/medicines/view?id=<%= r.getId() %>">
                <span class="<%= r.getThumbCssClass() %>" aria-hidden="true"><%= r.getThumbText() %></span>
                <strong><%= TextUtil.html(r.getDisplayName()) %></strong>
                <span class="meta"><%= TextUtil.html(r.getDosageForm()) %></span>
                <span class="price-row">
                    <span class="price"><%= TextUtil.money(r.getPrice()) %></span>
                    <% if (r.isRequiresPrescription()) { %><span class="badge badge-rx">Rx</span><% } %>
                </span>
            </a>
        <% } %>
    </div>
</section>
<% } %>

<script src="<%= ctx %>/js/cart.js" defer></script>
<%@ include file="../common/footer.jspf" %>
