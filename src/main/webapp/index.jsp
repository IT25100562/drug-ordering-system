<%--
    Home page.
    Staff go straight to their own start page; guests and customers see how
    MediSys works and where to begin.
    Shared file - agree with the team before changing it.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%
    // Staff have nothing to do on the shop front: send them to their work page.
    com.medisys.model.User homeUser = com.medisys.util.SessionUtil.currentUser(request);
    if (homeUser != null && !homeUser.isCustomer()) {
        String start;
        switch (homeUser.getRole()) {
            case ADMIN:
                start = "/admin/orders";
                break;
            case PHARMACIST:
                start = "/pharmacist/dashboard";
                break;
            default:
                start = "/staff/deliveries";
        }
        // Keep a message stored before the redirect (e.g. "That page is not available").
        response.sendRedirect(request.getContextPath() + start);
        return;
    }
%>
<% String pageTitle = "Home"; %>
<%@ include file="WEB-INF/views/common/header.jspf" %>

<section class="card home-hero">
    <div>
        <h1><%= currentUser == null ? "Your pharmacy, online" : "Welcome back, " + TextUtil.html(currentUser.getFirstName()) %></h1>
        <p class="subtitle">Order medicines from home. Prescription medicines are checked by our
            senior pharmacist first, and every order is delivered to your door.</p>
        <div class="actions">
            <a class="btn" href="<%= ctx %>/medicines">Browse medicines</a>
            <a class="btn plain" href="<%= ctx %>/prescriptions/upload">Upload a prescription</a>
            <% if (currentUser == null) { %>
                <a class="btn plain" href="<%= ctx %>/register">Create an account</a>
            <% } %>
        </div>
    </div>
</section>

<div class="home-steps">
    <section class="card">
        <span class="step-no">1</span>
        <h2>Find your medicines</h2>
        <p>Search the catalog, save favourites to your wishlist and add them to your cart.
            No account is needed to look around.</p>
    </section>
    <section class="card">
        <span class="step-no">2</span>
        <h2>Send your prescription</h2>
        <p>Upload a photo or PDF. The pharmacist reads it, lists the medicines and how to use
            them, and you pay online.</p>
    </section>
    <section class="card">
        <span class="step-no">3</span>
        <h2>Track the delivery</h2>
        <p>Delivery is Rs. 300, free from Rs. 2,500. Follow your parcel and get a notification
            at every step.</p>
    </section>
</div>

<% if (currentUser != null) { %>
<section class="card">
    <h2>Quick links</h2>
    <div class="actions">
        <a class="btn plain" href="<%= ctx %>/orders">My orders</a>
        <a class="btn plain" href="<%= ctx %>/prescriptions">My prescriptions</a>
        <a class="btn plain" href="<%= ctx %>/cart">My cart</a>
        <a class="btn plain" href="<%= ctx %>/account/profile">My profile</a>
    </div>
</section>
<% } %>

<%@ include file="WEB-INF/views/common/footer.jspf" %>
