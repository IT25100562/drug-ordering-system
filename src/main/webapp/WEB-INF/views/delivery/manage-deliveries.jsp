<%--
    Deliveries list with status tabs.
      - rider (DELIVERY_STAFF): the deliveries given to them, with quick "next step" buttons
      - admin: every delivery, and a rider picker for the ones still at the pharmacy
    Filled by ManageDeliveriesServlet (/staff/deliveries).

    Module : 06 - Delivery Tracking and Notification
    Owner  : Deshabhi R. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Delivery" %>
<%@ page import="com.medisys.model.DeliveryStatus" %>
<%@ page import="com.medisys.model.OrderStatus" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<% String pageTitle = "Deliveries"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Delivery> deliveries = (List<Delivery>) request.getAttribute("deliveries");
    @SuppressWarnings("unchecked")
    Map<String, Integer> counts = (Map<String, Integer>) request.getAttribute("counts");
    @SuppressWarnings("unchecked")
    List<User> riders = (List<User>) request.getAttribute("riders");   // admin only
    String filter = (String) request.getAttribute("filter");
    boolean admin = currentUser.isAdmin();

    java.util.List<String[]> tabs = new java.util.ArrayList<>();
    tabs.add(new String[]{"ACTIVE", "Open"});
    if (admin) {
        tabs.add(new String[]{"UNASSIGNED", "No rider yet"});
    }
    for (DeliveryStatus s : DeliveryStatus.values()) {
        tabs.add(new String[]{s.name(), s == DeliveryStatus.FAILED ? "Failed" : s.getLabel()});
    }
    tabs.add(new String[]{"ALL", "All"});
    int onTheRoad = counts.get("DISPATCHED") + counts.get("OUT_FOR_DELIVERY");
%>

<div class="title-row">
    <div>
        <h1><%= admin ? "Deliveries" : "My Deliveries" %></h1>
        <p class="subtitle"><%= admin
                ? "Give packed orders to a rider and follow every parcel until it arrives."
                : "Pick up your parcels, deliver them and keep the customer informed." %></p>
    </div>
</div>

<div class="stats">
    <% if (admin) { %>
        <a class="stat warn" href="<%= ctx %>/staff/deliveries?status=UNASSIGNED"><span><%= counts.get("UNASSIGNED") %></span>Need a rider</a>
    <% } else { %>
        <a class="stat warn" href="<%= ctx %>/staff/deliveries?status=PENDING"><span><%= counts.get("PENDING") %></span>To pick up</a>
    <% } %>
    <a class="stat" href="<%= ctx %>/staff/deliveries?status=ACTIVE"><span><%= onTheRoad %></span>On the road</a>
    <a class="stat danger" href="<%= ctx %>/staff/deliveries?status=FAILED"><span><%= counts.get("FAILED") %></span>Failed - try again</a>
    <a class="stat muted" href="<%= ctx %>/staff/deliveries?status=DELIVERED"><span><%= counts.get("DELIVERED") %></span>Delivered</a>
</div>

<section class="card">
    <nav class="filters" aria-label="Filter deliveries">
        <% for (String[] tab : tabs) { %>
            <a class="<%= tab[0].equals(filter) ? "active" : "" %>"
               href="<%= ctx %>/staff/deliveries?status=<%= tab[0] %>"
               <%= tab[0].equals(filter) ? "aria-current=\"page\"" : "" %>><%= tab[1] %> (<%= counts.get(tab[0]) %>)</a>
        <% } %>
    </nav>

    <% if (deliveries.isEmpty()) { %>
        <div class="empty"><%= "ACTIVE".equals(filter) ? "No open deliveries - all caught up." : "No deliveries in this list." %></div>
    <% } else { %>
    <div class="table-wrap">
    <table class="rx-table">
        <tr>
            <th>Order</th>
            <th>Deliver to</th>
            <th>Rider</th>
            <th>Status</th>
            <th><span class="sr-only">Actions</span></th>
        </tr>
        <% for (Delivery d : deliveries) {
               DeliveryStatus s = d.getStatus();
        %>
        <tr class="<%= s.isFinished() ? "row-muted" : "" %>">
            <td>
                <a href="<%= ctx %>/staff/deliveries/view?id=<%= d.getId() %>"><strong><%= d.getOrderReference() %></strong></a>
                <br><span class="meta">
                    <% if (s == DeliveryStatus.DELIVERED) { %>
                        Delivered <%= TextUtil.timeAgo(d.getDeliveredAt()) %>
                    <% } else if (!s.isFinished()) { %>
                        Due <%= TextUtil.date(d.getEstimatedDate()) %>
                    <% } %>
                </span>
                <% if (d.isLate()) { %><br><span class="badge badge-rejected">Late</span><% } %>
            </td>
            <td class="address-cell">
                <strong><%= TextUtil.html(d.getDeliveryName()) %></strong>
                <br><%= TextUtil.html(d.getDeliveryAddress()) %>
                <br><a href="tel:<%= TextUtil.html(d.getDeliveryPhone()) %>"><%= TextUtil.html(d.getDeliveryPhone()) %></a>
            </td>
            <td>
                <% if (admin && s.canAssignRider() && !riders.isEmpty()) { %>
                    <form class="assign-form" method="post" action="<%= ctx %>/staff/deliveries/update">
                        <input type="hidden" name="id" value="<%= d.getId() %>">
                        <input type="hidden" name="action" value="assign">
                        <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                        <label class="sr-only" for="rider-<%= d.getId() %>">Rider for <%= d.getOrderReference() %></label>
                        <select id="rider-<%= d.getId() %>" name="riderId" required>
                            <option value=""><%= d.hasRider() ? "Change rider" : "-- choose --" %></option>
                            <% for (User r : riders) { %>
                                <option value="<%= r.getId() %>" <%= Integer.valueOf(r.getId()).equals(d.getStaffId()) ? "selected" : "" %>>
                                    <%= TextUtil.html(r.getFullName()) %></option>
                            <% } %>
                        </select>
                        <button class="btn small" type="submit">Save</button>
                    </form>
                <% } else if (d.hasRider()) { %>
                    <%= TextUtil.html(d.getStaffName()) %>
                <% } else { %>
                    <span class="meta">-</span>
                <% } %>
            </td>
            <td>
                <span class="badge <%= s.getCssClass() %>"><%= s.getLabel() %></span>
                <% if (s == DeliveryStatus.PENDING) { %>
                    <br><span class="meta"><%= d.getOrderStatus() == OrderStatus.PROCESSING ? "Packed - ready" : "Not packed yet" %></span>
                <% } %>
                <% if (d.getAttempts() > 1) { %><br><span class="meta">Attempt <%= d.getAttempts() %></span><% } %>
            </td>
            <td class="row-actions">
                <% for (DeliveryStatus next : s.nextSteps()) {
                       // "Could not deliver" needs a reason, so it is on the delivery page.
                       boolean quick = next != DeliveryStatus.FAILED && d.hasRider()
                               && (next != DeliveryStatus.DISPATCHED || d.getOrderStatus() == OrderStatus.PROCESSING);
                       if (!quick) {
                           continue;
                       }
                %>
                    <form method="post" action="<%= ctx %>/staff/deliveries/update">
                        <input type="hidden" name="id" value="<%= d.getId() %>">
                        <input type="hidden" name="action" value="status">
                        <input type="hidden" name="current" value="<%= s.name() %>">
                        <input type="hidden" name="next" value="<%= next.name() %>">
                        <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                        <button class="btn small <%= next == DeliveryStatus.DELIVERED ? "approve" : "" %>" type="submit"><%= next.getActionLabel() %></button>
                    </form>
                <% } %>
                <a class="btn small plain" href="<%= ctx %>/staff/deliveries/view?id=<%= d.getId() %>">Open</a>
            </td>
        </tr>
        <% } %>
    </table>
    </div>
    <% } %>
</section>

<%@ include file="../common/footer.jspf" %>
