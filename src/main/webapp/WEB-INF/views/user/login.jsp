<%--
    Log in page, with a link to create an account.
    Filled by LoginServlet (/login).

    Module : Minor functions - User accounts and roles
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
        <p class="meta">Click a role to fill in the log-in form.</p>
        <table>
            <tr><th>Role</th><th>Email</th><th>Password</th></tr>
            <tr><td><button type="button" class="btn plain small" data-demo-email="nimal@example.com" data-demo-password="Customer@123">Customer</button></td><td>nimal@example.com</td><td>Customer@123</td></tr>
            <tr><td><button type="button" class="btn plain small" data-demo-email="kasuni@example.com" data-demo-password="Customer@123">Customer</button></td><td>kasuni@example.com</td><td>Customer@123</td></tr>
            <tr><td><button type="button" class="btn plain small" data-demo-email="tharindu@example.com" data-demo-password="Customer@123">Customer (flagged)</button></td><td>tharindu@example.com</td><td>Customer@123</td></tr>
            <tr><td><button type="button" class="btn plain small" data-demo-email="admin@medisys.lk" data-demo-password="Admin@123">Admin</button></td><td>admin@medisys.lk</td><td>Admin@123</td></tr>
            <tr><td><button type="button" class="btn plain small" data-demo-email="pharmacist@medisys.lk" data-demo-password="Pharma@123">Pharmacist</button></td><td>pharmacist@medisys.lk</td><td>Pharma@123</td></tr>
            <tr><td><button type="button" class="btn plain small" data-demo-email="delivery@medisys.lk" data-demo-password="Delivery@123">Delivery</button></td><td>delivery@medisys.lk</td><td>Delivery@123</td></tr>
        </table>
    </div>
</div>

<script>
    // Demo account buttons copy that account's email and password into the form.
    (function () {
        var email = document.getElementById("email");
        var password = document.getElementById("password");
        var submit = document.querySelector('.auth-card button[type="submit"]');
        document.querySelectorAll("[data-demo-email]").forEach(function (button) {
            button.addEventListener("click", function () {
                email.value = button.getAttribute("data-demo-email");
                password.value = button.getAttribute("data-demo-password");
                submit.focus();     // so pressing Enter logs straight in
            });
        });
    })();
</script>

<%@ include file="../common/footer.jspf" %>
