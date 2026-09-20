<%--
    Track Delivery: where the customer's order is right now.
    Filled by TrackDeliveryServlet (/deliveries/track?orderId=).

    Module : 06 - Delivery Tracking and Notification
    Owner  : Deshabhi R. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Delivery" %>
<%@ page import="com.medisys.model.DeliveryStatus" %>
<% String pageTitle = "Track Delivery"; %>
<%@ include file="../common/header.jspf" %>
<%
    Delivery delivery = (Delivery) request.getAttribute("delivery");
    DeliveryStatus status = delivery.getStatus();

    // One sentence that answers "where is my parcel?"
    String headline;
    switch (status) {
        case PENDING:
            headline = "We are preparing your parcel at the pharmacy.";
            break;
        case DISPATCHED:
            headline = "Your parcel has left the pharmacy.";
            break;
        case OUT_FOR_DELIVERY:
            headline = "Your parcel is on the way to you.";
            break;
        case DELIVERED:
            headline = "Your parcel was delivered.";
            break;
        case FAILED:
            headline = "We could not deliver your parcel. Our rider will try again.";
            break;
        default:
            headline = "This delivery was cancelled.";
    }
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/orders">My Orders</a> <span aria-hidden="true">/</span>
    <a href="<%= ctx %>/orders/view?id=<%= delivery.getOrderId() %>"><%= delivery.getOrderReference() %></a>
    <span aria-hidden="true">/</span>
    <span aria-current="page">Track</span>
</nav>

<div class="title-row">
    <div>
        <h1>Tracking order <%= delivery.getOrderReference() %></h1>
        <p class="subtitle">
            <span class="badge <%= status.getCssClass() %>"><%= status.getLabel() %></span>
            &nbsp;Last update <%= TextUtil.timeAgo(delivery.getUpdatedAt()) %>
        </p>
    </div>
    <a class="btn plain" href="<%= ctx %>/deliveries/track?orderId=<%= delivery.getOrderId() %>">Refresh</a>
</div>

<div class="track-hero card <%= status == DeliveryStatus.FAILED ? "problem" : "" %>">
    <div>
        <p class="track-headline"><%= headline %></p>
        <% if (status == DeliveryStatus.DELIVERED) { %>
            <p class="meta">Delivered <%= TextUtil.dateTime(delivery.getDeliveredAt()) %></p>
        <% } else if (!status.isFinished()) { %>
            <p class="meta">Expected by <strong><%= TextUtil.date(delivery.getEstimatedDate()) %></strong>
                <% if (delivery.isLate()) { %>
                    &middot; <span class="badge badge-rejected">Running late</span>
                    Sorry for the delay, it will reach you as soon as possible.
                <% } %>
            </p>
        <% } %>
        <% if (status == DeliveryStatus.FAILED && delivery.getLastUpdate() != null
               && delivery.getLastUpdate().getNote() != null) { %>
            <p class="note-box">Reason: <%= TextUtil.html(delivery.getLastUpdate().getNote()) %></p>
        <% } %>
    </div>
    <% if (delivery.getAttempts() > 1) { %>
        <span class="meta"><%= delivery.getAttempts() %> delivery attempts</span>
    <% } %>
</div>

<div class="view-layout">
    <div>
        <%@ include file="delivery-parts.jspf" %>
    </div>

    <aside>
        <section class="card">
            <h2>Your rider</h2>
            <% if (delivery.hasRider()) { %>
                <p class="rider"><span class="rider-icon" aria-hidden="true"><%= TextUtil.html(delivery.getStaffName().substring(0, 1)) %></span>
                    <span><strong><%= TextUtil.html(delivery.getStaffName()) %></strong>
                    <% if (delivery.getStaffPhone() != null && !status.isFinished()) { %>
                        <br><a href="tel:<%= TextUtil.html(delivery.getStaffPhone()) %>"><%= TextUtil.html(delivery.getStaffPhone()) %></a>
                    <% } %></span>
                </p>
            <% } else if (status == DeliveryStatus.CANCELLED) { %>
                <p class="meta">No rider - the order was cancelled.</p>
            <% } else { %>
                <p class="meta">A rider will pick up your parcel soon.</p>
            <% } %>
        </section>

        <section class="card">
            <h2>Delivering to</h2>
            <p><strong><%= TextUtil.html(delivery.getDeliveryName()) %></strong><br>
                <%= TextUtil.html(delivery.getDeliveryAddress()) %><br>
                <%= TextUtil.html(delivery.getDeliveryPhone()) %></p>
            <% if (delivery.getDeliveryNote() != null) { %>
                <p class="meta">Note for the rider: <%= TextUtil.html(delivery.getDeliveryNote()) %></p>
            <% } %>
        </section>

        <a class="btn plain block" href="<%= ctx %>/orders/view?id=<%= delivery.getOrderId() %>">View order details</a>
        <p class="meta center">Questions? Call the pharmacy on 011 234 5678.</p>
    </aside>
</div>

<%@ include file="../common/footer.jspf" %>
