<%--
    Admin view of one order: move it on, or cancel it with a reason.
    Filled by AdminOrderDetailsServlet (/admin/orders/view?id=).

    Module : 02 - Order Placement and Checkout
    Owner  : Hewage B. H. A. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Order" %>
<%@ page import="com.medisys.model.OrderStatus" %>
<%@ page import="com.medisys.model.Payment" %>
<% String pageTitle = "Order"; %>
<%@ include file="../common/header.jspf" %>
<%
    Order order = (Order) request.getAttribute("order");
    Payment payment = order.getPayment();
    OrderStatus next = order.getStatus().next();
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/admin/orders">Orders</a> <span aria-hidden="true">/</span>
    <span aria-current="page"><%= order.getReference() %></span>
</nav>

<div class="title-row">
    <div>
        <h1>Order <%= order.getReference() %></h1>
        <p class="subtitle">
            <span class="badge <%= order.getStatus().getCssClass() %>"><%= order.getStatus().getLabel() %></span>
            &nbsp;Placed <%= TextUtil.dateTime(order.getCreatedAt()) %> by <%= TextUtil.html(order.getCustomerName()) %>
            (<%= TextUtil.html(order.getCustomerEmail()) %>)
            <% if (order.isFromPrescription()) { %>
                &middot; <span class="badge badge-rx">Prescription <%= order.getPrescriptionReference() %></span>
            <% } %>
        </p>
    </div>
    <button class="btn plain" type="button" onclick="window.print()">Print packing slip</button>
</div>

<div class="view-layout">
    <div>
        <%@ include file="order-parts.jspf" %>
    </div>

    <aside>
        <% if (next != null) { %>
        <section class="card next-step">
            <h2>Next step</h2>
            <form method="post" action="<%= ctx %>/admin/orders/status">
                <input type="hidden" name="id" value="<%= order.getId() %>">
                <input type="hidden" name="action" value="advance">
                <input type="hidden" name="current" value="<%= order.getStatus().name() %>">
                <div class="field">
                    <label for="note">Note for the customer <span class="meta">(optional)</span></label>
                    <input type="text" id="note" name="note" maxlength="300"
                           placeholder="<%= next == OrderStatus.SHIPPED ? "e.g. Rider Kamal, 077 123 4567" : "" %>">
                </div>
                <button class="btn approve block" type="submit">Mark as &ldquo;<%= next.getLabel() %>&rdquo;</button>
            </form>
        </section>
        <% } %>

        <section class="card">
            <h2>Deliver to</h2>
            <p><strong><%= TextUtil.html(order.getDeliveryName()) %></strong><br>
                <%= TextUtil.html(order.getDeliveryAddress()) %><br>
                <a href="tel:<%= TextUtil.html(order.getDeliveryPhone()) %>"><%= TextUtil.html(order.getDeliveryPhone()) %></a></p>
            <% if (order.getDeliveryNote() != null) { %>
                <p class="note-box"><%= TextUtil.html(order.getDeliveryNote()) %></p>
            <% } %>
        </section>

        <section class="card">
            <h2>Payment</h2>
            <dl class="info compact">
                <dt>Reference</dt><dd><%= TextUtil.html(payment.getReference()) %></dd>
                <dt>Card</dt><dd>&bull;&bull;&bull;&bull; <%= TextUtil.html(payment.getCardLast4()) %></dd>
                <dt>Amount</dt><dd><%= TextUtil.money(payment.getAmount()) %></dd>
                <dt>Status</dt><dd><%= payment.isRefunded() ? "Refunded " + TextUtil.dateTime(payment.getRefundedAt()) : "Paid" %></dd>
            </dl>
        </section>

        <% if (order.getStatus().canBeCancelledByPharmacy()) { %>
        <section class="card">
            <h2>Cancel order</h2>
            <p class="meta">Only before it leaves the pharmacy. Stock goes back and the customer is refunded.</p>
            <form method="post" action="<%= ctx %>/admin/orders/status"
                  data-confirm="Cancel <%= order.getReference() %> and refund <%= TextUtil.money(order.getTotal()) %>?">
                <input type="hidden" name="id" value="<%= order.getId() %>">
                <input type="hidden" name="action" value="cancel">
                <div class="field">
                    <label for="reason">Reason for the customer *</label>
                    <input type="text" id="reason" name="reason" minlength="5" maxlength="300" required
                           placeholder="e.g. A medicine was damaged in storage">
                </div>
                <button class="btn reject block" type="submit">Cancel and refund</button>
            </form>
        </section>
        <% } %>
    </aside>
</div>

<%@ include file="../common/footer.jspf" %>
