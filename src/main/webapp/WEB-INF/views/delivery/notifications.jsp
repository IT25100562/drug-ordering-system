<%--
    The logged in user's notifications, newest first.
    Filled by NotificationsServlet (/notifications).
    Delete / Clear read post to DeleteNotificationServlet.

    Module : 06 - Delivery Tracking and Notification
    Owner  : Deshabhi R. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Notification" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "Notifications"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
%>

<div class="title-row">
    <div>
        <h1>Notifications</h1>
        <p class="subtitle"><%= currentUser.isDeliveryStaff()
                ? "Deliveries given to you."
                : "Messages about your prescriptions, orders and deliveries." %></p>
    </div>
    <% if (!notifications.isEmpty()) { %>
        <form method="post" action="<%= ctx %>/notifications/delete"
              data-confirm="Clear all notifications you have already read?">
            <input type="hidden" name="all" value="read">
            <button class="btn plain" type="submit">Clear read</button>
        </form>
    <% } %>
</div>

<section class="card">
    <% if (notifications.isEmpty()) { %>
        <div class="empty">You have no notifications yet.</div>
    <% } %>
    <% for (Notification n : notifications) {
           boolean hasLink = TextUtil.isSafeLocalPath(n.getLink());
    %>
        <div class="notification <%= n.isRead() ? "" : "unread" %>">
            <% if (!n.isRead()) { %><span class="badge badge-pending">New</span><% } %>
            <p><%= TextUtil.html(n.getMessage()) %></p>
            <span class="time" title="<%= TextUtil.dateTime(n.getCreatedAt()) %>"><%= TextUtil.timeAgo(n.getCreatedAt()) %></span>
            <% if (hasLink) { %>
                &middot; <a href="<%= ctx %><%= TextUtil.html(n.getLink()) %>">Open</a>
            <% } %>
            <form class="inline-form" method="post" action="<%= ctx %>/notifications/delete">
                <input type="hidden" name="id" value="<%= n.getId() %>">
                &middot; <button class="link-button danger" type="submit">Delete</button>
            </form>
        </div>
    <% } %>
</section>

<%@ include file="../common/footer.jspf" %>
