<%--
    Deliveries in three tabs:
      New         - new parcels at the pharmacy. Any rider presses "Got the package".
      On the way  - picked up. "Delivered" or "Could not deliver" (then "Try again").
      Completed   - delivered or cancelled.
    A rider sees their own deliveries plus new ones nobody has taken yet.
    The admin sees all of them, read only: riders work directly with new parcels.
    Filled by ManageDeliveriesServlet (/staff/deliveries?tab=).

    Module : 06 - Delivery Tracking and Notification
    Owner  : Deshabhi R. G. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Delivery" %>
<%@ page import="com.medisys.model.DeliveryStatus" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<% String pageTitle = "Deliveries"; %>
<%@ include file="../common/header.jspf" %>
<%
    @SuppressWarnings("unchecked")
    List<Delivery> deliveries = (List<Delivery>) request.getAttribute("deliveries");
    @SuppressWarnings("unchecked")
    Map<String, Integer> counts = (Map<String, Integer>) request.getAttribute("counts");
    String filter = (String) request.getAttribute("filter");
    boolean admin = currentUser.isAdmin();

    String[][] tabs = {
            {"NEW", "New deliveries"},
            {"ON_THE_WAY", "On the way"},
            {"COMPLETED", "Completed"}};
    String empty = "NEW".equals(filter) ? "No new deliveries right now."
            : "ON_THE_WAY".equals(filter) ? "Nothing on the way. Pick up a parcel from New deliveries."
            : "No completed deliveries yet.";
%>

<div class="title-row">
    <div>
        <h1><%= admin ? "Deliveries" : "My Deliveries" %></h1>
        <p class="subtitle"><%= admin
                ? "Riders pick up new parcels themselves. Follow every parcel here."
                : "Pick up a parcel in New deliveries, then mark it delivered in On the way." %></p>
    </div>
</div>

<section class="card">
    <nav class="filters" aria-label="Delivery lists">
        <% for (String[] tab : tabs) { %>
            <a class="<%= tab[0].equals(filter) ? "active" : "" %>"
               href="<%= ctx %>/staff/deliveries?tab=<%= tab[0] %>"
               <%= tab[0].equals(filter) ? "aria-current=\"page\"" : "" %>><%= tab[1] %> (<%= counts.get(tab[0]) %>)</a>
        <% } %>
    </nav>

    <% if (deliveries.isEmpty()) { %>
        <div class="empty"><%= empty %></div>
    <% } else { %>
    <div class="table-wrap">
    <table class="rx-table">
        <tr>
            <th>Order</th>
            <th>Deliver to</th>
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
                <span class="badge <%= s.getCssClass() %>"><%= s == DeliveryStatus.PENDING ? "New" : s.getLabel() %></span>
                <% if (admin && d.hasRider()) { %><br><span class="meta">Rider: <%= TextUtil.html(d.getStaffName()) %></span><% } %>
                <% if (d.getAttempts() > 1) { %><br><span class="meta">Attempt <%= d.getAttempts() %></span><% } %>
            </td>
            <td class="row-actions">
                <% if (!admin && s == DeliveryStatus.PENDING) { %>
                    <form method="post" action="<%= ctx %>/staff/deliveries/update">
                        <input type="hidden" name="id" value="<%= d.getId() %>">
                        <input type="hidden" name="action" value="pickup">
                        <input type="hidden" name="current" value="<%= s.name() %>">
                        <input type="hidden" name="returnTo" value="/staff/deliveries?tab=ON_THE_WAY">
                        <button class="btn small approve" type="submit">Got the package</button>
                    </form>
                <% } else if (!admin && !s.isFinished()) { %>
                    <%-- On the way: one button per next step. "Could not deliver" needs a
                         reason, so it opens the delivery page. --%>
                    <% for (DeliveryStatus next : s.nextSteps()) { %>
                        <% if (next == DeliveryStatus.FAILED) { %>
                            <a class="btn small reject" href="<%= ctx %>/staff/deliveries/view?id=<%= d.getId() %>#reason">Could not deliver</a>
                        <% } else { %>
                            <form method="post" action="<%= ctx %>/staff/deliveries/update">
                                <input type="hidden" name="id" value="<%= d.getId() %>">
                                <input type="hidden" name="action" value="status">
                                <input type="hidden" name="current" value="<%= s.name() %>">
                                <input type="hidden" name="next" value="<%= next.name() %>">
                                <input type="hidden" name="returnTo" value="<%= TextUtil.html(currentUrl) %>">
                                <button class="btn small <%= next == DeliveryStatus.DELIVERED ? "approve" : "" %>" type="submit">
                                    <%= s == DeliveryStatus.FAILED ? "Try again" : next.getActionLabel() %></button>
                            </form>
                        <% } %>
                    <% } %>
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
