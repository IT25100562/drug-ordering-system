<%--
    One delivery for the rider (next step buttons) or the admin (read only):
    address, the medicines to hand over, and the tracking history.
    Filled by DeliveryDetailsServlet (/staff/deliveries/view?id=).

    Module : 06 - Delivery Tracking and Notification
    Owner  : Deshabhi R. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Delivery" %>
<%@ page import="com.medisys.model.DeliveryStatus" %>
<%@ page import="com.medisys.model.Order" %>
<%@ page import="com.medisys.model.OrderItem" %>
<% String pageTitle = "Delivery"; %>
<%@ include file="../common/header.jspf" %>
<%
    Delivery delivery = (Delivery) request.getAttribute("delivery");
    Order order = (Order) request.getAttribute("order");
    boolean admin = currentUser.isAdmin();   // the admin can look, only riders change deliveries
    DeliveryStatus status = delivery.getStatus();
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/staff/deliveries?tab=<%= status == DeliveryStatus.PENDING ? "NEW" : status.isFinished() ? "COMPLETED" : "ON_THE_WAY" %>"><%= admin ? "Deliveries" : "My Deliveries" %></a> <span aria-hidden="true">/</span>
    <span aria-current="page"><%= delivery.getOrderReference() %></span>
</nav>

<div class="title-row">
    <div>
        <h1>Delivery for <%= delivery.getOrderReference() %></h1>
        <p class="subtitle">
            <span class="badge <%= status.getCssClass() %>"><%= status.getLabel() %></span>
            <% if (!status.isFinished()) { %>
                &nbsp;Due <%= TextUtil.date(delivery.getEstimatedDate()) %>
                <% if (delivery.isLate()) { %><span class="badge badge-rejected">Late</span><% } %>
            <% } else if (status == DeliveryStatus.DELIVERED) { %>
                &nbsp;Delivered <%= TextUtil.dateTime(delivery.getDeliveredAt()) %>
            <% } %>
            <% if (delivery.getAttempts() > 0) { %>
                &middot; <%= delivery.getAttempts() %> attempt<%= delivery.getAttempts() == 1 ? "" : "s" %>
            <% } %>
            <% if (admin) { %>
                &middot; <a href="<%= ctx %>/admin/orders/view?id=<%= delivery.getOrderId() %>">Open the order</a>
            <% } %>
        </p>
    </div>
</div>

<div class="view-layout">
    <div>
        <%@ include file="delivery-parts.jspf" %>

        <% if (order != null) { %>
        <section class="card">
            <h2>Parcel contents</h2>
            <div class="table-wrap">
            <table class="items-table">
                <tr><th>Medicine</th><th class="num">Qty</th></tr>
                <% for (OrderItem item : order.getItems()) { %>
                    <tr>
                        <td><strong><%= TextUtil.html(item.getMedicineName()) %></strong>
                            <span class="meta"><%= TextUtil.html(item.getDosageForm()) %></span></td>
                        <td class="num"><%= item.getQuantity() %></td>
                    </tr>
                <% } %>
            </table>
            </div>
            <p class="meta">Payment was taken online - do not collect cash.
                <% if (order.isFromPrescription()) { %>Prescription medicines: hand them only to the customer or an adult at the address.<% } %></p>
        </section>
        <% } %>
    </div>

    <aside>
        <% if (admin && !status.isFinished()) { %>
        <section class="card">
            <h2>Next step</h2>
            <p class="meta"><%= status == DeliveryStatus.PENDING
                    ? "Waiting for a rider. Any rider can pick this parcel up from their New deliveries list."
                    : "The rider updates this delivery until it is delivered." %></p>
        </section>
        <% } else if (!status.nextSteps().isEmpty()) { %>
        <section class="card next-step">
            <h2>Next step</h2>
            <% if (status == DeliveryStatus.PENDING) { %>
                <form method="post" action="<%= ctx %>/staff/deliveries/update" class="step-form">
                    <input type="hidden" name="id" value="<%= delivery.getId() %>">
                    <input type="hidden" name="action" value="pickup">
                    <input type="hidden" name="current" value="<%= status.name() %>">
                    <p class="meta">Press this when you have the parcel in your hands. The customer is told it is on the way.</p>
                    <button class="btn block approve" type="submit">Got the package</button>
                </form>
            <% } else { %>
                <% for (DeliveryStatus next : status.nextSteps()) {
                       boolean failing = next == DeliveryStatus.FAILED;
                %>
                    <form method="post" action="<%= ctx %>/staff/deliveries/update" class="step-form">
                        <input type="hidden" name="id" value="<%= delivery.getId() %>">
                        <input type="hidden" name="action" value="status">
                        <input type="hidden" name="current" value="<%= status.name() %>">
                        <input type="hidden" name="next" value="<%= next.name() %>">
                        <div class="field">
                            <% if (failing) { %>
                                <label for="reason">Why could it not be delivered? *</label>
                                <input type="text" id="reason" name="note" minlength="5" maxlength="300" required
                                       placeholder="e.g. Nobody at home, phone not answered">
                            <% } else { %>
                                <label for="note-<%= next.name() %>">Note for the customer <span class="meta">(optional)</span></label>
                                <input type="text" id="note-<%= next.name() %>" name="note" maxlength="300"
                                       placeholder="<%= next == DeliveryStatus.DELIVERED ? "e.g. Handed to the security guard" : "e.g. Arriving in about 30 minutes" %>">
                            <% } %>
                        </div>
                        <button class="btn block <%= failing ? "reject" : "approve" %>" type="submit">
                            <%= status == DeliveryStatus.FAILED ? "Try again" : next.getActionLabel() %></button>
                    </form>
                <% } %>
            <% } %>
        </section>
        <% } %>

        <% if (delivery.hasRider()) { %>
        <section class="card">
            <h2>Rider</h2>
            <p><strong><%= TextUtil.html(delivery.getStaffName()) %></strong>
                <% if (delivery.getStaffPhone() != null) { %><br><%= TextUtil.html(delivery.getStaffPhone()) %><% } %></p>
        </section>
        <% } %>

        <section class="card">
            <h2>Deliver to</h2>
            <p><strong><%= TextUtil.html(delivery.getDeliveryName()) %></strong><br>
                <%= TextUtil.html(delivery.getDeliveryAddress()) %><br>
                <a href="tel:<%= TextUtil.html(delivery.getDeliveryPhone()) %>"><%= TextUtil.html(delivery.getDeliveryPhone()) %></a></p>
            <% if (delivery.getDeliveryNote() != null) { %>
                <p class="note-box"><%= TextUtil.html(delivery.getDeliveryNote()) %></p>
            <% } %>
            <p class="meta">Customer account: <%= TextUtil.html(delivery.getCustomerName()) %></p>
        </section>
    </aside>
</div>

<%@ include file="../common/footer.jspf" %>
