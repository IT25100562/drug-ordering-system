<%--
    Admin list of all orders with status tabs, search and quick "next step" buttons.
    Filled by ManageOrdersServlet (/admin/orders).

    Module : 02 - Order Placement and Checkout
    Owner  : Hewage B. H. A. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Order" %>
<%@ page import="com.medisys.model.OrderStatus" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<% String pageTitle = "Orders"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Order> orders = (List<Order>) request.getAttribute("orders");
    @SuppressWarnings("unchecked")
    Map<String, Integer> counts = (Map<String, Integer>) request.getAttribute("counts");
    String filter = (String) request.getAttribute("filter");
    String keyword = (String) request.getAttribute("keyword");
    String keep = keyword.isEmpty() ? "" : "&q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);

    String[][] tabs = {
            {"OPEN", "Open"},
            {"PAID", "New"},
            {"PROCESSING", "Being packed"},
            {"SHIPPED", "Out for delivery"},
            {"DELIVERED", "Delivered"},
            {"CANCELLED", "Cancelled"},
            {"ALL", "All"}
    };
%>

<div class="title-row">
    <div>
        <h1>Orders</h1>
        <p class="subtitle">Pack new orders, then give them to a rider on the
            <a href="<%= ctx %>/staff/deliveries">Deliveries</a> page.</p>
    </div>
</div>

<div class="stats">
    <a class="stat warn" href="<%= ctx %>/admin/orders?status=PAID"><span><%= counts.get("PAID") %></span>New - to pack</a>
    <a class="stat" href="<%= ctx %>/admin/orders?status=PROCESSING"><span><%= counts.get("PROCESSING") %></span>Being packed</a>
    <a class="stat" href="<%= ctx %>/admin/orders?status=SHIPPED"><span><%= counts.get("SHIPPED") %></span>Out for delivery</a>
    <a class="stat muted" href="<%= ctx %>/admin/orders?status=DELIVERED"><span><%= counts.get("DELIVERED") %></span>Delivered</a>
</div>

<section class="card">
    <nav class="filters" aria-label="Filter orders">
        <% for (String[] tab : tabs) { %>
            <a class="<%= tab[0].equals(filter) ? "active" : "" %>"
               href="<%= ctx %>/admin/orders?status=<%= tab[0] %><%= keep %>"
               <%= tab[0].equals(filter) ? "aria-current=\"page\"" : "" %>><%= tab[1] %> (<%= counts.get(tab[0]) %>)</a>
        <% } %>
    </nav>

    <form class="search-bar" method="get" action="<%= ctx %>/admin/orders" role="search">
        <input type="hidden" name="status" value="<%= TextUtil.html(filter) %>">
        <input type="search" name="q" value="<%= TextUtil.html(keyword) %>" maxlength="100"
               placeholder="Order number, customer name or email" aria-label="Search orders">
        <button class="btn" type="submit">Search</button>
        <% if (!keyword.isEmpty()) { %><a class="btn plain" href="<%= ctx %>/admin/orders?status=<%= filter %>">Clear</a><% } %>
    </form>

    <% if (orders.isEmpty()) { %>
        <div class="empty"><%= "PAID".equals(filter) ? "No new orders - all caught up." : "No orders in this list." %></div>
    <% } else { %>
    <div class="table-wrap">
    <table class="rx-table">
        <tr>
            <th>Order</th>
            <th>Customer</th>
            <th>Medicines</th>
            <th class="num">Total</th>
            <th>Status</th>
            <th><span class="sr-only">Actions</span></th>
        </tr>
        <% for (Order o : orders) {
               OrderStatus next = o.getStatus().nextForPharmacy();   // later steps: module 06 (Deliveries)
        %>
        <tr class="<%= o.isCancelled() ? "row-muted" : "" %>">
            <td>
                <a href="<%= ctx %>/admin/orders/view?id=<%= o.getId() %>"><strong><%= o.getReference() %></strong></a>
                <br><span class="meta" title="<%= TextUtil.dateTime(o.getCreatedAt()) %>"><%= TextUtil.timeAgo(o.getCreatedAt()) %></span>
                <% if (o.isFromPrescription()) { %><br><span class="badge badge-rx"><%= o.getPrescriptionReference() %></span><% } %>
            </td>
            <td>
                <%= TextUtil.html(o.getCustomerName()) %>
                <br><span class="meta"><%= TextUtil.html(o.getDeliveryPhone()) %></span>
            </td>
            <td class="order-items-cell"><%= TextUtil.html(o.getItemSummary()) %></td>
            <td class="num nowrap"><%= TextUtil.money(o.getTotal()) %></td>
            <td><span class="badge <%= o.getStatus().getCssClass() %>"><%= o.getStatus().getLabel() %></span></td>
            <td class="row-actions">
                <% if (next != null) { %>
                    <form method="post" action="<%= ctx %>/admin/orders/status">
                        <input type="hidden" name="id" value="<%= o.getId() %>">
                        <input type="hidden" name="action" value="advance">
                        <input type="hidden" name="current" value="<%= o.getStatus().name() %>">
                        <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                        <button class="btn small" type="submit">Mark: <%= next.getLabel() %></button>
                    </form>
                <% } %>
                <a class="btn small plain" href="<%= ctx %>/admin/orders/view?id=<%= o.getId() %>">Open</a>
            </td>
        </tr>
        <% } %>
    </table>
    </div>
    <% } %>
</section>

<%@ include file="../common/footer.jspf" %>
