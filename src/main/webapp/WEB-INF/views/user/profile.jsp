<%--
    My Profile: photo, details, password, and (customers) the latest
    prescriptions and orders.
    Filled by ProfileServlet (/account/profile). The photo form posts to
    ProfilePhotoServlet (/account/photo).

    Module : 04 - User and Role Management
    Owner  : Kaweesha P. M. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Order" %>
<%@ page import="com.medisys.model.Prescription" %>
<%@ page import="com.medisys.service.UserService" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<% String pageTitle = "My Profile"; %>
<%@ include file="../common/header.jspf" %>
<%
    User profile = (User) request.getAttribute("profile");
    String errorForm = (String) request.getAttribute("errorForm");
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
    @SuppressWarnings("unchecked")
    Map<String, String> form = (Map<String, String>) request.getAttribute("form");
    @SuppressWarnings("unchecked")
    List<Prescription> prescriptions = (List<Prescription>) request.getAttribute("prescriptions");
    @SuppressWarnings("unchecked")
    List<Order> orders = (List<Order>) request.getAttribute("orders");

    // After a failed save, show what was typed; otherwise the saved values.
    boolean detailsFailed = "details".equals(errorForm);
    String nameValue = detailsFailed ? form.get("fullName") : profile.getFullName();
    String phoneValue = detailsFailed ? form.get("phone") : profile.getPhone();
    String whatsappValue = detailsFailed ? form.get("whatsapp") : profile.getWhatsapp();
    String addressValue = detailsFailed ? form.get("address") : profile.getAddress();
%>
<%!
    private String errorList(List<String> errors) {
        StringBuilder html = new StringBuilder("<div class=\"message error\" role=\"alert\"><ul>");
        for (String e : errors) {
            html.append("<li>").append(TextUtil.html(e)).append("</li>");
        }
        return html.append("</ul></div>").toString();
    }
%>

<div class="profile-head card">
    <%= avatar(ctx, profile, "avatar xl") %>
    <div class="profile-name">
        <h1><%= TextUtil.html(profile.getFullName()) %></h1>
        <p class="subtitle"><%= TextUtil.html(profile.getRole().getLabel()) %>
            &middot; member since <%= TextUtil.date(profile.getCreatedAt().toLocalDate()) %></p>
        <form class="photo-form" method="post" action="<%= ctx %>/account/photo" enctype="multipart/form-data">
            <label class="btn small plain" for="photo"><%= profile.hasPhoto() ? "Change photo" : "Add a photo" %></label>
            <input class="sr-only" type="file" id="photo" name="photo" accept="image/jpeg,image/png"
                   onchange="this.form.submit()">
            <noscript><button class="btn small" type="submit">Upload</button></noscript>
            <span class="meta">JPG or PNG, up to 2 MB</span>
        </form>
        <% if (profile.hasPhoto()) { %>
            <form class="photo-form" method="post" action="<%= ctx %>/account/photo" enctype="multipart/form-data"
                  data-confirm="Remove your profile photo?">
                <input type="hidden" name="action" value="remove">
                <button class="link-button danger" type="submit">Remove photo</button>
            </form>
        <% } %>
    </div>
    <% if (profile.isCustomer()) { %>
        <div class="profile-links">
            <a class="btn" href="<%= ctx %>/prescriptions/upload">Upload prescription</a>
            <a class="btn plain" href="<%= ctx %>/medicines">Browse medicines</a>
        </div>
    <% } %>
</div>

<div class="profile-layout">
    <div>
        <% if (profile.isCustomer()) { %>
        <%-- ------------------------------------------- latest prescriptions --%>
        <section class="card">
            <div class="section-head">
                <h2>My prescriptions</h2>
                <a href="<%= ctx %>/prescriptions">See all</a>
            </div>
            <% if (prescriptions.isEmpty()) { %>
                <p class="meta">No prescriptions yet. <a href="<%= ctx %>/prescriptions/upload">Upload one</a>
                    and our pharmacist will list your medicines.</p>
            <% } else { %>
            <ul class="history-list">
                <% for (Prescription p : prescriptions) { %>
                    <li>
                        <a href="<%= ctx %>/prescriptions/view?id=<%= p.getId() %>"><strong><%= p.getReference() %></strong></a>
                        <span class="meta"><%= TextUtil.dateTime(p.getUploadedAt()) %></span>
                        <span class="grow meta"><%= p.getItems().isEmpty() ? "" : TextUtil.html(p.getMedicineNames()) %></span>
                        <% if (p.isPaid()) { %>
                            <span class="badge badge-paid">Paid</span>
                        <% } else { %>
                            <span class="badge <%= p.getStatus().getCssClass() %>"><%= p.getStatus().getLabel() %></span>
                        <% } %>
                    </li>
                <% } %>
            </ul>
            <% } %>
        </section>

        <%-- ------------------------------------------------- latest orders --%>
        <section class="card">
            <div class="section-head">
                <h2>My orders</h2>
                <a href="<%= ctx %>/orders">See all</a>
            </div>
            <% if (orders.isEmpty()) { %>
                <p class="meta">No orders yet. <a href="<%= ctx %>/medicines">Browse medicines</a>.</p>
            <% } else { %>
            <ul class="history-list">
                <% for (Order o : orders) { %>
                    <li>
                        <a href="<%= ctx %>/orders/view?id=<%= o.getId() %>"><strong><%= o.getReference() %></strong></a>
                        <span class="meta"><%= TextUtil.dateTime(o.getCreatedAt()) %></span>
                        <span class="grow"><%= TextUtil.money(o.getTotal()) %></span>
                        <span class="badge <%= o.getStatus().getCssClass() %>"><%= o.getStatus().getLabel() %></span>
                        <% if (!o.isCancelled()) { %>
                            <a class="btn small plain" href="<%= ctx %>/deliveries/track?orderId=<%= o.getId() %>">Track</a>
                        <% } %>
                    </li>
                <% } %>
            </ul>
            <% } %>
        </section>
        <% } %>

        <%-- ------------------------------------------------------ details --%>
        <section class="card" id="details">
            <h2>My details</h2>
            <% if (detailsFailed) { %><%= errorList(errors) %><% } %>
            <form method="post" action="<%= ctx %>/account/profile">
                <input type="hidden" name="action" value="details">
                <div class="field">
                    <label for="fullName">Full name *</label>
                    <input type="text" id="fullName" name="fullName" value="<%= TextUtil.html(nameValue) %>"
                           maxlength="100" required>
                </div>
                <div class="row">
                    <div class="field">
                        <label for="phone">Phone number <%= profile.isCustomer() ? "*" : "" %></label>
                        <input type="tel" id="phone" name="phone" value="<%= TextUtil.html(phoneValue) %>"
                               maxlength="15" <%= profile.isCustomer() ? "required" : "" %>>
                    </div>
                    <div class="field">
                        <label for="whatsapp">WhatsApp number</label>
                        <input type="tel" id="whatsapp" name="whatsapp" value="<%= TextUtil.html(whatsappValue) %>"
                               maxlength="15" placeholder="0771234567">
                    </div>
                </div>
                <div class="field">
                    <label for="address">Delivery address</label>
                    <textarea id="address" name="address" rows="2" maxlength="255"
                              placeholder="House number, street, city"><%= TextUtil.html(addressValue) %></textarea>
                    <div class="hint">Filled in for you at checkout.</div>
                </div>
                <button class="btn" type="submit">Save details</button>
            </form>
        </section>
    </div>

    <aside>
        <section class="card">
            <h2>Account</h2>
            <dl class="info compact">
                <dt>Email</dt><dd><%= TextUtil.html(profile.getEmail()) %></dd>
                <% if (profile.getNic() != null) { %>
                    <dt>NIC</dt><dd><%= TextUtil.html(profile.getNic()) %></dd>
                <% } %>
                <% if (profile.getDateOfBirth() != null) { %>
                    <dt>Born</dt><dd><%= TextUtil.date(profile.getDateOfBirth()) %>
                        <span class="meta">(<%= profile.getAge() %>)</span></dd>
                <% } %>
            </dl>
            <p class="meta">To correct your email, NIC or date of birth, please call the pharmacy on 011 234 5678.</p>
        </section>

        <section class="card" id="password">
            <h2>Change password</h2>
            <% if ("password".equals(errorForm)) { %><%= errorList(errors) %><% } %>
            <form method="post" action="<%= ctx %>/account/profile">
                <input type="hidden" name="action" value="password">
                <div class="field">
                    <label for="currentPassword">Current password</label>
                    <input type="password" id="currentPassword" name="currentPassword"
                           autocomplete="current-password" required>
                </div>
                <div class="field">
                    <label for="newPassword">New password</label>
                    <input type="password" id="newPassword" name="newPassword" minlength="<%= UserService.PASSWORD_MIN %>"
                           maxlength="100" autocomplete="new-password" required>
                    <div class="hint">At least <%= UserService.PASSWORD_MIN %> characters, with a letter and a number.</div>
                </div>
                <div class="field">
                    <label for="confirmPassword">Repeat new password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword"
                           maxlength="100" autocomplete="new-password" required>
                </div>
                <button class="btn plain block" type="submit">Change password</button>
            </form>
        </section>
    </aside>
</div>

<%@ include file="../common/footer.jspf" %>
