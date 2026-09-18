<%--
    Customer registration: only what the pharmacy needs to know who you are.
    Filled by RegisterServlet (/register).

    Module : Minor functions - User accounts and roles
    Owner  : Kaweesha P. M. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.service.UserService" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<% String pageTitle = "Create an account"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    Map<String, String> form = (Map<String, String>) request.getAttribute("form");
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
    String returnTo = (String) request.getAttribute("returnTo");
    String loginUrl = ctx + "/login"
            + (returnTo.isEmpty() ? "" : "?returnTo=" + URLEncoder.encode(returnTo, StandardCharsets.UTF_8));
    // The latest birthday that is old enough, for the date picker.
    String maxBirthDate = java.time.LocalDate.now().minusYears(UserService.MIN_AGE).toString();
    boolean sameAsPhone = form.isEmpty() || "on".equals(form.get("sameAsPhone"));
%>
<%!
    private String value(Map<String, String> form, String field) {
        return TextUtil.html(form.get(field));
    }
%>

<div class="register-wrap">
    <section class="card">
        <h1>Create your account</h1>
        <p class="subtitle"><%= returnTo.startsWith("/prescriptions")
                ? "Register to upload your prescription. It takes a minute."
                : "Register to order medicines and upload prescriptions." %></p>

        <% if (errors != null && !errors.isEmpty()) { %>
            <div class="message error" role="alert">
                <strong>Please check the following:</strong>
                <ul><% for (String e : errors) { %><li><%= TextUtil.html(e) %></li><% } %></ul>
            </div>
        <% } %>

        <form method="post" action="<%= ctx %>/register" data-register-form>
            <input type="hidden" name="returnTo" value="<%= TextUtil.html(returnTo) %>">

            <fieldset class="form-section">
                <legend>About you</legend>
                <div class="field">
                    <label for="fullName">Full name *</label>
                    <input type="text" id="fullName" name="fullName" value="<%= value(form, "fullName") %>"
                           maxlength="100" autocomplete="name" required autofocus
                           placeholder="As on your NIC, e.g. Nimal Perera">
                </div>
                <div class="row">
                    <div class="field">
                        <label for="nic">NIC number *</label>
                        <input type="text" id="nic" name="nic" value="<%= value(form, "nic") %>"
                               maxlength="12" required pattern="[0-9]{9}[VvXx]|[0-9]{12}"
                               placeholder="951234567V or 199512345678">
                        <div class="hint">Old (9 digits + V) or new (12 digits) NIC.</div>
                    </div>
                    <div class="field">
                        <label for="dateOfBirth">Date of birth *</label>
                        <input type="date" id="dateOfBirth" name="dateOfBirth" value="<%= value(form, "dateOfBirth") %>"
                               max="<%= maxBirthDate %>" min="1900-01-01" autocomplete="bday" required>
                        <div class="hint">You must be <%= UserService.MIN_AGE %> or older.</div>
                    </div>
                </div>
            </fieldset>

            <fieldset class="form-section">
                <legend>How we can reach you</legend>
                <div class="row">
                    <div class="field">
                        <label for="phone">Phone number *</label>
                        <input type="tel" id="phone" name="phone" value="<%= value(form, "phone") %>"
                               maxlength="15" autocomplete="tel" required placeholder="0771234567">
                    </div>
                    <div class="field">
                        <label for="whatsapp">WhatsApp number</label>
                        <input type="tel" id="whatsapp" name="whatsapp" value="<%= value(form, "whatsapp") %>"
                               maxlength="15" placeholder="0771234567" <%= sameAsPhone ? "disabled" : "" %>>
                        <label class="checkbox-inline">
                            <input type="checkbox" name="sameAsPhone" id="sameAsPhone" <%= sameAsPhone ? "checked" : "" %>>
                            Same as my phone number
                        </label>
                    </div>
                </div>
                <div class="field">
                    <label for="email">Email *</label>
                    <input type="email" id="email" name="email" value="<%= value(form, "email") %>"
                           maxlength="150" autocomplete="email" required placeholder="you@example.com">
                    <div class="hint">You log in with this email.</div>
                </div>
            </fieldset>

            <fieldset class="form-section">
                <legend>Password</legend>
                <div class="row">
                    <div class="field">
                        <label for="password">Password *</label>
                        <input type="password" id="password" name="password" minlength="<%= UserService.PASSWORD_MIN %>"
                               maxlength="100" autocomplete="new-password" required>
                        <div class="hint">At least <%= UserService.PASSWORD_MIN %> characters, with a letter and a number.</div>
                    </div>
                    <div class="field">
                        <label for="confirmPassword">Repeat password *</label>
                        <input type="password" id="confirmPassword" name="confirmPassword"
                               maxlength="100" autocomplete="new-password" required>
                    </div>
                </div>
            </fieldset>

            <button class="btn block" type="submit">Create account</button>
            <p class="meta center">Already registered? <a href="<%= TextUtil.html(loginUrl) %>">Log in</a></p>
        </form>
    </section>

    <aside class="card help-card">
        <h2>Why we ask</h2>
        <ul class="tips">
            <li><strong>NIC and date of birth</strong> let our pharmacist check that a prescription
                belongs to you.</li>
            <li><strong>Phone and WhatsApp</strong> are used when the pharmacist or the rider needs
                to contact you.</li>
            <li>Only the pharmacy staff can see these details.</li>
        </ul>
    </aside>
</div>

<script>
    // "Same as my phone number" turns the WhatsApp box off (the server copies the phone).
    (function () {
        var box = document.getElementById("sameAsPhone");
        var whatsapp = document.getElementById("whatsapp");
        box.addEventListener("change", function () {
            whatsapp.disabled = box.checked;
            if (!box.checked) {
                whatsapp.focus();
            }
        });
    })();
</script>

<%@ include file="../common/footer.jspf" %>
