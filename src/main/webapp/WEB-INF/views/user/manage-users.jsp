<%--
    Admin: who uses MediSys. Photo, name, email, role, joined date and the red flag.
    Personal details (NIC, date of birth, phone) are deliberately NOT shown here;
    the pharmacist sees them next to each prescription.
    Also: add a staff account, switch staff accounts off / on.
    Filled by ManageUsersServlet (/admin/users).

    Module : 04 - User and Role Management
    Owner  : Kaweesha P. M. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Role" %>
<%@ page import="com.medisys.service.UserService" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<% String pageTitle = "Users"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<User> users = (List<User>) request.getAttribute("users");
    @SuppressWarnings("unchecked")
    Map<String, Integer> counts = (Map<String, Integer>) request.getAttribute("counts");
    @SuppressWarnings("unchecked")
    Map<String, String> form = (Map<String, String>) request.getAttribute("form");
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
    String filter = (String) request.getAttribute("filter");
    String keyword = (String) request.getAttribute("keyword");
    String keep = keyword.isEmpty() ? "" : "&q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);

    String[][] tabs = {
            {"ALL", "Everyone"},
            {"CUSTOMERS", "Customers"},
            {"FLAGGED", "Flagged"},
            {"STAFF", "Staff"}
    };
%>

<div class="title-row">
    <div>
        <h1>Users</h1>
        <p class="subtitle">Everyone who has an account. Customer details are shown to the pharmacist
            with each prescription.</p>
    </div>
    <a class="btn" href="#add-staff">+ Add staff account</a>
</div>

<div class="stats">
    <a class="stat" href="<%= ctx %>/admin/users?show=CUSTOMERS"><span><%= counts.get("CUSTOMERS") %></span>Customers</a>
    <a class="stat danger" href="<%= ctx %>/admin/users?show=FLAGGED"><span><%= counts.get("FLAGGED") %></span>Flagged</a>
    <a class="stat muted" href="<%= ctx %>/admin/users?show=STAFF"><span><%= counts.get("STAFF") %></span>Staff</a>
</div>

<section class="card">
    <nav class="filters" aria-label="Filter users">
        <% for (String[] tab : tabs) { %>
            <a class="<%= tab[0].equals(filter) ? "active" : "" %>"
               href="<%= ctx %>/admin/users?show=<%= tab[0] %><%= keep %>"
               <%= tab[0].equals(filter) ? "aria-current=\"page\"" : "" %>><%= tab[1] %> (<%= counts.get(tab[0]) %>)</a>
        <% } %>
    </nav>

    <form class="search-bar" method="get" action="<%= ctx %>/admin/users" role="search">
        <input type="hidden" name="show" value="<%= TextUtil.html(filter) %>">
        <input type="search" name="q" value="<%= TextUtil.html(keyword) %>" maxlength="100"
               placeholder="Name or email" aria-label="Search users">
        <button class="btn" type="submit">Search</button>
        <% if (!keyword.isEmpty()) { %><a class="btn plain" href="<%= ctx %>/admin/users?show=<%= filter %>">Clear</a><% } %>
    </form>

    <% if (users.isEmpty()) { %>
        <div class="empty">No users in this list.</div>
    <% } else { %>
    <div class="table-wrap">
    <table class="rx-table users-table">
        <tr>
            <th>User</th>
            <th>Role</th>
            <th>Joined</th>
            <th>Status</th>
            <th><span class="sr-only">Actions</span></th>
        </tr>
        <% for (User u : users) { %>
        <tr class="<%= u.isFlagged() ? "row-flagged" : "" %> <%= u.isActive() ? "" : "row-muted" %>">
            <td>
                <div class="person">
                    <%= avatar(ctx, u, "avatar") %>
                    <span><strong><%= TextUtil.html(u.getFullName()) %></strong>
                        <% if (u.getId() == currentUser.getId()) { %><span class="meta">(you)</span><% } %>
                        <br><span class="meta"><%= TextUtil.html(u.getEmail()) %></span></span>
                </div>
            </td>
            <td><%= TextUtil.html(u.getRole().getLabel()) %></td>
            <td class="nowrap"><span title="<%= TextUtil.dateTime(u.getCreatedAt()) %>"><%= TextUtil.date(u.getCreatedAt().toLocalDate()) %></span></td>
            <td>
                <% if (u.isFlagged()) { %>
                    <span class="badge flag-badge">&#9873; Flagged</span>
                    <br><span class="meta flag-reason"><%= TextUtil.html(u.getFlagReason()) %>
                        (<%= TextUtil.html(u.getFlaggedByName()) %>)</span>
                <% } else if (!u.isActive()) { %>
                    <span class="badge badge-expired">Switched off</span>
                <% } else { %>
                    <span class="badge badge-approved">Active</span>
                <% } %>
            </td>
            <td class="row-actions">
                <% if (u.isFlagged()) { %>
                    <form method="post" action="<%= ctx %>/users/flag"
                          data-confirm="Remove the red flag from <%= TextUtil.html(u.getFullName()) %>?">
                        <input type="hidden" name="id" value="<%= u.getId() %>">
                        <input type="hidden" name="action" value="unflag">
                        <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                        <button class="btn small plain" type="submit">Remove flag</button>
                    </form>
                <% } %>
                <% if (!u.isCustomer() && u.getId() != currentUser.getId()) { %>
                    <form method="post" action="<%= ctx %>/admin/users"
                          <%= u.isActive() ? "data-confirm=\"Switch off " + TextUtil.html(u.getFullName()) + "'s account? They will not be able to log in.\"" : "" %>>
                        <input type="hidden" name="id" value="<%= u.getId() %>">
                        <input type="hidden" name="action" value="<%= u.isActive() ? "deactivate" : "activate" %>">
                        <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                        <button class="btn small <%= u.isActive() ? "plain danger-text" : "" %>" type="submit">
                            <%= u.isActive() ? "Switch off" : "Switch on" %></button>
                    </form>
                <% } %>
                <% if (!u.isCustomer()) { %>
                    <details class="reset-box">
                        <summary>Reset password</summary>
                        <form method="post" action="<%= ctx %>/admin/users">
                            <input type="hidden" name="id" value="<%= u.getId() %>">
                            <input type="hidden" name="action" value="reset">
                            <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                            <label class="sr-only" for="newPassword-<%= u.getId() %>">New password for <%= TextUtil.html(u.getFullName()) %></label>
                            <input type="text" id="newPassword-<%= u.getId() %>" name="password" autocomplete="off"
                                   minlength="<%= UserService.PASSWORD_MIN %>" maxlength="100" required placeholder="New password">
                            <button class="btn small" type="submit">Save</button>
                        </form>
                    </details>
                <% } %>
            </td>
        </tr>
        <% } %>
    </table>
    </div>
    <% } %>
    <p class="meta">Customers are never switched off. A pharmacist can flag a customer who misuses
        MediSys - they can still order and upload.</p>
</section>

<section class="card" id="add-staff">
    <h2>Add a staff account</h2>
    <% if (errors != null && !errors.isEmpty()) { %>
        <div class="message error" role="alert">
            <ul><% for (String e : errors) { %><li><%= TextUtil.html(e) %></li><% } %></ul>
        </div>
    <% } %>
    <form method="post" action="<%= ctx %>/admin/users">
        <input type="hidden" name="action" value="create">
        <div class="row">
            <div class="field">
                <label for="fullName">Full name *</label>
                <input type="text" id="fullName" name="fullName" value="<%= TextUtil.html(form.get("fullName")) %>"
                       maxlength="100" required>
            </div>
            <div class="field">
                <label for="role">Role *</label>
                <select id="role" name="role" required>
                    <option value="">-- choose --</option>
                    <% for (Role r : new Role[]{Role.PHARMACIST, Role.DELIVERY_STAFF, Role.ADMIN}) { %>
                        <option value="<%= r.name() %>" <%= r.name().equals(form.get("role")) ? "selected" : "" %>><%= r.getLabel() %></option>
                    <% } %>
                </select>
            </div>
        </div>
        <div class="row">
            <div class="field">
                <label for="staffEmail">Email *</label>
                <input type="email" id="staffEmail" name="email" value="<%= TextUtil.html(form.get("email")) %>"
                       maxlength="150" required>
            </div>
            <div class="field">
                <label for="staffPhone">Phone</label>
                <input type="tel" id="staffPhone" name="phone" value="<%= TextUtil.html(form.get("phone")) %>"
                       maxlength="15" placeholder="0771234567">
            </div>
        </div>
        <div class="field">
            <label for="staffPassword">First password *</label>
            <input type="text" id="staffPassword" name="password" minlength="<%= UserService.PASSWORD_MIN %>"
                   maxlength="100" autocomplete="off" required>
            <div class="hint">At least <%= UserService.PASSWORD_MIN %> characters with a letter and a number.
                They can change it on their profile page.</div>
        </div>
        <button class="btn" type="submit">Create account</button>
    </form>
</section>

<%@ include file="../common/footer.jspf" %>
