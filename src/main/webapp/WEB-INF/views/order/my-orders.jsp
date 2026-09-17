<%--
    Customer's order history.
    Filled by MyOrdersServlet (/orders).

    Module : 02 - Order Placement and Checkout
    Owner  : Hewage B. H. A. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Order" %>
<%@ page import="java.util.List" %>
<% String pageTitle = "My Orders"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Order> orders = (List<Order>) request.getAttribute("orders");
%>

<div class="title-row">
    <div>
        <h1>My Orders</h1>
        <p class="subtitle">Everything you ordered, newest first.</p>
    </div>
    <a class="btn plain" href="<%= ctx %>/medicines">Browse medicines</a>
</div>

<% if (orders.isEmpty()) { %>
    <div class="card empty-state">
        <h2>No orders yet</h2>
        <p>When you check out your cart or pay for a prescription, your orders appear here.</p>
        <a class="btn" href="<%= ctx %>/medicines">Start shopping</a>
    </div>
<% } %>

<div class="order-list">
<% for (Order o : orders) { %>
    <article class="card order-card">
        <div class="order-card-head">
            <div>
                <h2><a href="<%= ctx %>/orders/view?id=<%= o.getId() %>">Order <%= o.getReference() %></a></h2>
                <span class="meta">Placed <%= TextUtil.dateTime(o.getCreatedAt()) %>
                    <% if (o.isFromPrescription()) { %> &middot; prescription <%= o.getPrescriptionReference() %><% } %>
                </span>
            </div>
            <span class="badge <%= o.getStatus().getCssClass() %>"><%= o.getStatus().getLabel() %></span>
        </div>
        <div class="order-thumbs">
            <% int shown = 0;
               for (com.medisys.model.OrderItem item : o.getItems()) {
                   if (shown++ == 4) { %><span class="more">+<%= o.getItems().size() - 4 %></span><% break; } %>
                <span class="<%= item.getThumbCssClass() %>" title="<%= TextUtil.html(item.getMedicineName()) %>"><%= item.getThumbText() %></span>
            <% } %>
            <p class="order-summary-text"><%= TextUtil.html(o.getItemSummary()) %></p>
        </div>
        <div class="order-card-foot">
            <span><strong><%= TextUtil.money(o.getTotal()) %></strong>
                <span class="meta">&middot; <%= o.getPackCount() %> pack<%= o.getPackCount() == 1 ? "" : "s" %></span></span>
            <span class="actions">
                <% if (!o.isFromPrescription()) { %>
                    <form method="post" action="<%= ctx %>/orders/reorder">
                        <input type="hidden" name="id" value="<%= o.getId() %>">
                        <button class="btn small plain" type="submit">Buy again</button>
                    </form>
                <% } %>
                <a class="btn small" href="<%= ctx %>/orders/view?id=<%= o.getId() %>">View order</a>
            </span>
        </div>
    </article>
<% } %>
</div>

<%@ include file="../common/footer.jspf" %>
