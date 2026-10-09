<%--
    Home page (the shop front). Filled by HomeServlet (GET /).
    Shared file - agree with the team before changing it.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.medicine.Category" %>
<%@ page import="com.medisys.medicine.Medicine" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "Online pharmacy"; %>
<%@ include file="header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Category> categories = (List<Category>) request.getAttribute("categories");
    @SuppressWarnings("unchecked")
    List<Medicine> featured = (List<Medicine>) request.getAttribute("featured");
%>

<section class="hero">
    <div class="hero-text">
        <span class="eyebrow">Licensed online pharmacy &middot; Sri Lanka</span>
        <h1><%= currentUser == null ? "Your pharmacy, delivered to your door"
                                   : "Welcome back, " + TextUtil.html(currentUser.getFirstName()) %></h1>
        <p>Order genuine medicines from home. Prescriptions are checked by our pharmacist,
            and every order is tracked until it reaches you.</p>

        <form class="hero-search" method="get" action="<%= ctx %>/medicines" role="search">
            <label class="sr-only" for="hero-q">Search medicines</label>
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M10 2a8 8 0 0 1 6.32 12.9l5.39 5.4-1.41 1.4-5.4-5.38A8 8 0 1 1 10 2zm0 2a6 6 0 1 0 0 12 6 6 0 0 0 0-12z"/></svg>
            <input type="search" id="hero-q" name="q" placeholder="Search Panadol, Vitamin C, cough syrup..." autocomplete="off">
            <button class="btn" type="submit">Search</button>
        </form>

        <div class="hero-actions">
            <a class="btn light" href="<%= ctx %>/prescriptions/upload">
                <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 13h8v2H8zm0 4h5v2H8z"/></svg>
                Upload a prescription</a>
            <% if (currentUser == null) { %>
                <a class="hero-link" href="<%= ctx %>/register">Create a free account &rarr;</a>
            <% } else { %>
                <a class="hero-link" href="<%= ctx %>/orders">Track my orders &rarr;</a>
            <% } %>
        </div>
    </div>

    <div class="hero-art" aria-hidden="true">
        <% int shown = 0;
           for (Medicine m : featured) {
               if (!m.hasImage() || shown == 3) { continue; }
               shown++; %>
            <img class="hero-pack hero-pack-<%= shown %>" src="<%= TextUtil.html(m.getImageUrl(ctx, 480)) %>" alt="">
        <% } %>
    </div>
</section>

<ul class="trust-strip">
    <li><span class="trust-icon">&#10003;</span><div><strong>Pharmacist verified</strong>Every prescription is checked</div></li>
    <li><span class="trust-icon">&#9733;</span><div><strong>Genuine medicines</strong>From licensed suppliers</div></li>
    <li><span class="trust-icon">&#10148;</span><div><strong>Island-wide delivery</strong>Free from Rs. 2,500</div></li>
    <li><span class="trust-icon">&#9679;</span><div><strong>Live tracking</strong>Updates at every step</div></li>
</ul>

<section class="home-section">
    <div class="section-title">
        <h2>Shop by category</h2>
        <a href="<%= ctx %>/medicines">All medicines &rarr;</a>
    </div>
    <div class="category-tiles">
        <% for (Category c : categories) { %>
            <a class="category-tile" href="<%= ctx %>/medicines?category=<%= c.getId() %>">
                <span class="category-initial"><%= TextUtil.html(c.getName().substring(0, 1)) %></span>
                <span class="category-label"><%= TextUtil.html(c.getName()) %>
                    <small><%= c.getMedicineCount() %> <%= c.getMedicineCount() == 1 ? "product" : "products" %></small></span>
            </a>
        <% } %>
    </div>
</section>

<% if (!featured.isEmpty()) { %>
<section class="home-section">
    <div class="section-title">
        <h2>Popular right now</h2>
        <a href="<%= ctx %>/medicines">See the full catalog &rarr;</a>
    </div>
    <div class="featured-grid">
        <% for (Medicine m : featured) { %>
            <a class="featured-card" href="<%= ctx %>/medicines/view?id=<%= m.getId() %>">
                <span class="<%= m.getThumbCssClass() %>"><%= thumbInner(ctx, m, 200) %></span>
                <span class="featured-category"><%= TextUtil.html(m.getCategoryName()) %></span>
                <strong><%= TextUtil.html(m.getDisplayName()) %></strong>
                <span class="price"><%= TextUtil.money(m.getPrice()) %></span>
            </a>
        <% } %>
    </div>
</section>
<% } %>

<section class="how-it-works">
    <h2>How MediSys works</h2>
    <ol>
        <li><span class="step-no">1</span><strong>Find your medicines</strong>
            Search the catalog, save favourites to your wishlist and add them to your cart.</li>
        <li><span class="step-no">2</span><strong>Send your prescription</strong>
            Upload a photo or PDF. Our pharmacist lists the medicines and how to use them.</li>
        <li><span class="step-no">3</span><strong>Pay and relax</strong>
            Pay online, then follow your parcel until the rider hands it to you.</li>
    </ol>
</section>

<%@ include file="footer.jspf" %>
