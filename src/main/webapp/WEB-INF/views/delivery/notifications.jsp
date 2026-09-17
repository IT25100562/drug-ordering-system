<%--
    The logged in user's notifications, newest first.
    Filled by NotificationsServlet (/notifications).

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
        <p class="subtitle">Messages about your prescriptions and orders.</p>
    </div>
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
        </div>
    <% } %>
</section>

<%@ include file="../common/footer.jspf" %>
