<%--
    Log in page, with a link to create an account.
    Filled by LoginServlet (/login).

    Module : 04 - User and Role Management
    Owner  : Kaweesha P. M. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<% String pageTitle = "Log in"; %>
<%@ include file="../common/header.jspf" %>
<%
    String error = (String) request.getAttribute("error");
    String email = (String) request.getAttribute("email");
    String returnTo = (String) request.getAttribute("returnTo");
    String registerUrl = ctx + "/register"
            + (returnTo.isEmpty() ? "" : "?returnTo=" + URLEncoder.encode(returnTo, StandardCharsets.UTF_8));
    boolean forPrescription = returnTo.startsWith("/prescriptions");
%>

<div class="auth-wrap">
    <div class="card auth-card">
        <h1>Log in</h1>
        <p class="subtitle"><%= forPrescription
                ? "Log in to upload your prescription."
                : "Welcome to MediSys. Log in to use your cart, prescriptions and orders." %></p>

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
            <p class="meta center"><a href="<%= ctx %>/forgot-password">Forgot your password?</a></p>
        </form>

        <div class="auth-switch">
            <span>New to MediSys?</span>
            <a class="btn plain block" href="<%= TextUtil.html(registerUrl) %>">Create a free account</a>
            <span class="meta">You can browse medicines without an account. You need one to order
                or to upload a prescription.</span>
        </div>
    </div>

    <%-- Demo accounts for testing and the presentation (see database/sample-data.sql). --%>
    <div class="card demo-accounts">
        <h2>Demo accounts</h2>
        <table>
            <tr><th>Role</th><th>Email</th><th>Password</th></tr>
            <tr><td>Customer</td><td>nimal@example.com</td><td>Customer@123</td></tr>
            <tr><td>Customer</td><td>kasuni@example.com</td><td>Customer@123</td></tr>
            <tr><td>Customer (flagged)</td><td>tharindu@example.com</td><td>Customer@123</td></tr>
            <tr><td>Admin</td><td>admin@medisys.lk</td><td>Admin@123</td></tr>
            <tr><td>Pharmacist</td><td>pharmacist@medisys.lk</td><td>Pharma@123</td></tr>
            <tr><td>Delivery</td><td>delivery@medisys.lk</td><td>Delivery@123</td></tr>
        </table>
    </div>
</div>

<%@ include file="../common/footer.jspf" %>
