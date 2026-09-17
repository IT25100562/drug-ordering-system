<%--
    Log in page.
    Filled by LoginServlet (/login).

    Module : 04 - User and Role Management
    Owner  : Kaweesha P. M. G. S.

    TODO (module 04): link to the registration page once it exists.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "Log in"; %>
<%@ include file="../common/header.jspf" %>
<%
    String error = (String) request.getAttribute("error");
    String email = (String) request.getAttribute("email");
    String returnTo = (String) request.getAttribute("returnTo");
%>

<div class="auth-wrap">
    <div class="card auth-card">
        <h1>Log in</h1>
        <p class="subtitle">Welcome to MediSys. Log in to use your cart and wishlist.</p>

        <% if (error != null) { %>
            <div class="message error" role="alert"><%= TextUtil.html(error) %></div>
        <% } %>

        <form method="post" action="<%= ctx %>/login">
            <input type="hidden" name="returnTo" value="<%= TextUtil.html(returnTo) %>">
            <div class="field">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" value="<%= TextUtil.html(email) %>"
                       autocomplete="username" required autofocus>
            </div>
            <div class="field">
                <label for="password">Password</label>
                <input type="password" id="password" name="password"
                       autocomplete="current-password" required>
            </div>
            <button class="btn block" type="submit">Log in</button>
        </form>
    </div>

    <%-- Demo accounts for testing and the presentation (see database/sample-data.sql). --%>
    <div class="card demo-accounts">
        <h2>Demo accounts</h2>
        <table>
            <tr><th>Role</th><th>Email</th><th>Password</th></tr>
            <tr><td>Customer</td><td>nimal@example.com</td><td>Customer@123</td></tr>
            <tr><td>Customer</td><td>kasuni@example.com</td><td>Customer@123</td></tr>
            <tr><td>Admin</td><td>admin@medisys.lk</td><td>Admin@123</td></tr>
            <tr><td>Pharmacist</td><td>pharmacist@medisys.lk</td><td>Pharma@123</td></tr>
            <tr><td>Delivery</td><td>delivery@medisys.lk</td><td>Delivery@123</td></tr>
        </table>
    </div>
</div>

<%@ include file="../common/footer.jspf" %>
