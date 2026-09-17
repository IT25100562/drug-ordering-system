<%--
    One of the customer's orders: progress, medicines, payment and delivery.
    Right after checkout it also works as the confirmation page.
    Filled by OrderDetailsServlet (/orders/view?id=).

    Module : 02 - Order Placement and Checkout
    Owner  : Hewage B. H. A. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Order" %>
<%@ page import="com.medisys.model.Payment" %>
<% String pageTitle = "Order"; %>
<%@ include file="../common/header.jspf" %>
<%
    Order order = (Order) request.getAttribute("order");
    boolean justPlaced = (Boolean) request.getAttribute("justPlaced");
    Payment payment = order.getPayment();
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/orders">My Orders</a> <span aria-hidden="true">/</span>
    <span aria-current="page"><%= order.getReference() %></span>
</nav>

<% if (justPlaced) { %>
    <ol class="checkout-steps" aria-label="Checkout steps">
        <li class="done">Cart</li>
        <li class="done">Delivery &amp; payment</li>
        <li class="done">Confirmation</li>
    </ol>
    <div class="card thank-you">
        <div class="big-tick" aria-hidden="true">&#10003;</div>
        <div>
            <h1>Thank you for your order!</h1>
            <p>Order <strong><%= order.getReference() %></strong> is confirmed and paid.
                We will notify you when it is packed and when it is on its way.</p>
        </div>
    </div>
<% } %>

<div class="title-row">
    <div>
        <% if (justPlaced) { %>
            <h2>Order <%= order.getReference() %></h2>
        <% } else { %>
            <h1>Order <%= order.getReference() %></h1>
        <% } %>
        <p class="subtitle">
            <span class="badge <%= order.getStatus().getCssClass() %>"><%= order.getStatus().getLabel() %></span>
            &nbsp;Placed <%= TextUtil.dateTime(order.getCreatedAt()) %>
            <% if (order.isFromPrescription()) { %>
                &middot; from prescription
                <a href="<%= ctx %>/prescriptions/view?id=<%= order.getPrescriptionId() %>"><%= order.getPrescriptionReference() %></a>
            <% } %>
        </p>
    </div>
    <div class="actions">
        <% if (!order.isFromPrescription()) { %>
            <form method="post" action="<%= ctx %>/orders/reorder">
                <input type="hidden" name="id" value="<%= order.getId() %>">
                <button class="btn plain" type="submit">Buy again</button>
            </form>
        <% } %>
        <button class="btn plain" type="button" onclick="window.print()">Print</button>
    </div>
</div>

<div class="view-layout">
    <div>
        <%@ include file="order-parts.jspf" %>
    </div>

    <aside>
        <section class="card">
            <h2>Delivery</h2>
            <p><strong><%= TextUtil.html(order.getDeliveryName()) %></strong><br>
                <%= TextUtil.html(order.getDeliveryAddress()) %><br>
                <%= TextUtil.html(order.getDeliveryPhone()) %></p>
            <% if (order.getDeliveryNote() != null) { %>
                <p class="meta">Note: <%= TextUtil.html(order.getDeliveryNote()) %></p>
            <% } %>
        </section>

        <section class="card">
            <h2>Payment</h2>
            <dl class="info compact">
                <dt>Reference</dt><dd><strong><%= TextUtil.html(payment.getReference()) %></strong></dd>
                <dt>Card</dt><dd>&bull;&bull;&bull;&bull; <%= TextUtil.html(payment.getCardLast4()) %></dd>
                <dt>Paid</dt><dd><%= TextUtil.money(payment.getAmount()) %><br>
                    <span class="meta"><%= TextUtil.dateTime(payment.getPaidAt()) %></span></dd>
                <% if (payment.isRefunded()) { %>
                    <dt>Refunded</dt><dd><span class="badge badge-approved">Refunded</span><br>
                        <span class="meta"><%= TextUtil.dateTime(payment.getRefundedAt()) %></span></dd>
                <% } %>
            </dl>
        </section>

        <% if (order.getStatus().canBeCancelledByCustomer()) { %>
        <section class="card">
            <h2>Changed your mind?</h2>
            <p class="meta">You can cancel until we start packing. The full amount is refunded.</p>
            <form method="post" action="<%= ctx %>/orders/cancel"
                  data-confirm="Cancel order <%= order.getReference() %>? This cannot be undone.">
                <input type="hidden" name="id" value="<%= order.getId() %>">
                <div class="field">
                    <label for="reason">Reason <span class="meta">(optional)</span></label>
                    <select id="reason" name="reason">
                        <option value="">-- choose --</option>
                        <option>Ordered by mistake</option>
                        <option>Found it cheaper elsewhere</option>
                        <option>Delivery takes too long</option>
                        <option>Doctor changed the medicine</option>
                        <option>Other</option>
                    </select>
                </div>
                <button class="btn reject block" type="submit">Cancel order</button>
            </form>
        </section>
        <% } else if (!order.isCancelled() && order.getStatus() != com.medisys.model.OrderStatus.DELIVERED) { %>
            <p class="meta center">Your order is already being prepared, so it can no longer be cancelled
                online. Please call the pharmacy on 011 234 5678.</p>
        <% } %>
    </aside>
</div>

<%@ include file="../common/footer.jspf" %>
