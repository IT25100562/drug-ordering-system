<%--
    Forgot password: prove who you are (email + NIC + date of birth) and
    choose a new password.
    Filled by ForgotPasswordServlet (/forgot-password).

    Module : Minor functions - User accounts and roles
    Owner  : Kaweesha P. M. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.service.UserService" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "Forgot password"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
%>

<div class="auth-wrap single">
    <div class="card auth-card">
        <h1>Forgot your password?</h1>
        <p class="subtitle">Enter the details you registered with, then choose a new password.</p>

        <% if (errors != null && !errors.isEmpty()) { %>
            <div class="message error" role="alert">
                <ul><% for (String e : errors) { %><li><%= TextUtil.html(e) %></li><% } %></ul>
            </div>
        <% } %>

        <form method="post" action="<%= ctx %>/forgot-password">
            <div class="field">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" value="<%= TextUtil.html(request.getAttribute("email")) %>"
                       autocomplete="username" required autofocus>
            </div>
            <div class="row">
                <div class="field">
                    <label for="nic">NIC number</label>
                    <input type="text" id="nic" name="nic" value="<%= TextUtil.html(request.getAttribute("nic")) %>"
                           maxlength="12" required>
                </div>
                <div class="field">
                    <label for="dateOfBirth">Date of birth</label>
                    <input type="date" id="dateOfBirth" name="dateOfBirth"
                           value="<%= TextUtil.html(request.getAttribute("dateOfBirth")) %>" required>
                </div>
            </div>
            <div class="field">
                <label for="password">New password</label>
                <input type="password" id="password" name="password" minlength="<%= UserService.PASSWORD_MIN %>"
                       maxlength="100" autocomplete="new-password" required>
                <div class="hint">At least <%= UserService.PASSWORD_MIN %> characters, with a letter and a number.</div>
            </div>
            <div class="field">
                <label for="confirmPassword">Repeat new password</label>
                <input type="password" id="confirmPassword" name="confirmPassword" maxlength="100"
                       autocomplete="new-password" required>
            </div>
            <button class="btn block" type="submit">Set new password</button>
        </form>
        <p class="meta center">Remembered it? <a href="<%= ctx %>/login">Log in</a> &middot;
            Staff: ask the administrator to reset your password.</p>
    </div>
</div>

<%@ include file="../common/footer.jspf" %>
