<%--
    One of the customer's prescriptions. Once approved it shows the medicines
    with how to use them, the total and the Pay button; after payment it is the
    receipt.
    Filled by ViewPrescriptionServlet (/prescriptions/view?id=).

    Module : 05 - Prescription Upload and Verification
    Owner  : Perera D. A. A. N. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Prescription" %>
<%@ page import="com.medisys.model.PrescriptionStatus" %>
<%@ page import="com.medisys.model.OrderStatus" %>
<%@ page import="com.medisys.service.OrderService" %>
<%@ page import="java.math.BigDecimal" %>
<% String pageTitle = "Prescription"; %>
<%@ include file="../common/header.jspf" %>
<%
    Prescription p = (Prescription) request.getAttribute("prescription");
    PrescriptionStatus status = p.getStatus();
    boolean hasItems = !p.getItems().isEmpty();
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/prescriptions">My Prescriptions</a> <span aria-hidden="true">/</span>
    <span aria-current="page"><%= p.getReference() %></span>
</nav>

<div class="title-row">
    <div>
        <h1>Prescription <%= p.getReference() %></h1>
        <p class="subtitle">
            <% if (p.isPaid()) { %>
                <span class="badge badge-paid">Paid</span>
            <% } else { %>
                <span class="badge <%= status.getCssClass() %>"><%= status.getLabel() %></span>
            <% } %>
            <% if (p.isExpired()) { %><span class="badge badge-expired">Expired</span><% } %>
            &nbsp;Uploaded <%= TextUtil.dateTime(p.getUploadedAt()) %>
        </p>
    </div>
    <a class="file-chip inline" href="<%= ctx %>/prescriptions/file?id=<%= p.getId() %>" target="_blank" rel="noopener">
        <span class="file-type"><%= p.isPdf() ? "PDF" : "IMG" %></span>
        <span><%= TextUtil.html(p.getOriginalFileName()) %><br><span class="meta">View your file</span></span>
    </a>
</div>

<div class="<%= hasItems ? "view-layout" : "" %>">
    <section class="card">
        <% if (hasItems) { %>
            <h2>Your medicines</h2>
            <p class="subtitle">Listed by <%= TextUtil.html(p.getReviewedByName()) %> (Senior Pharmacist)
                on <%= TextUtil.dateTime(p.getReviewedAt()) %>.</p>
            <%@ include file="items-table.jspf" %>
            <p class="safety-note">Take your medicines exactly as shown above. Ask our pharmacist if
                anything is unclear, and keep all medicines out of reach of children.</p>
        <% } else if (status == PrescriptionStatus.PENDING) { %>
            <h2>Being checked</h2>
            <p>Our senior pharmacist is reading your prescription. When it is approved you will get a
                notification, and this page will show your medicines, how to use them and the total to pay.</p>
        <% } else if (p.needsCorrection()) { %>
            <h2>New copy needed</h2>
            <p>Our pharmacist could not use this copy. Please send a clearer or corrected one.</p>
            <a class="btn correction" href="<%= ctx %>/prescriptions/correct?id=<%= p.getId() %>">Send corrected copy</a>
        <% } else if (status == PrescriptionStatus.REJECTED) { %>
            <h2>Not accepted</h2>
            <p>Our pharmacist could not accept this prescription. You can upload a new one.</p>
            <a class="btn" href="<%= ctx %>/prescriptions/upload">Upload a new prescription</a>
        <% } %>

        <% if (p.getPharmacistNote() != null && status != PrescriptionStatus.PENDING) { %>
            <div class="note-box">
                <span class="note-title">Note from our pharmacist</span>
                <p><%= TextUtil.html(p.getPharmacistNote()) %></p>
            </div>
        <% } %>
        <% if (p.getCustomerNote() != null) { %>
            <p class="meta">Your note: <%= TextUtil.html(p.getCustomerNote()) %></p>
        <% } %>
    </section>

    <% if (hasItems) { %>
    <aside class="card pay-box">
        <% if (p.isPaid()) { %>
            <h2>Receipt</h2>
            <dl class="info compact">
                <dt>Reference</dt><dd><strong><%= TextUtil.html(p.getPaymentReference()) %></strong></dd>
                <dt>Order</dt><dd><a href="<%= ctx %>/orders/view?id=<%= p.getOrderId() %>"><%= p.getOrderReference() %></a></dd>
                <dt>Amount</dt><dd><%= TextUtil.money(p.getAmountPaid()) %> <span class="meta">(incl. delivery)</span></dd>
                <dt>Paid on</dt><dd><%= TextUtil.dateTime(p.getPaidAt()) %></dd>
                <dt>Card</dt><dd>&bull;&bull;&bull;&bull; <%= TextUtil.html(p.getCardLast4()) %></dd>
                <dt>Deliver to</dt><dd><%= TextUtil.html(p.getDeliveryName()) %><br>
                    <%= TextUtil.html(p.getDeliveryAddress()) %><br><%= TextUtil.html(p.getDeliveryPhone()) %></dd>
            </dl>
            <% OrderStatus orderStatus = OrderStatus.fromText(p.getOrderStatus()); %>
            <p class="paid-note">&#10003; Order <%= p.getOrderReference() %>:
                <%= orderStatus == null ? "" : orderStatus.getLabel() %></p>
            <a class="btn block" href="<%= ctx %>/orders/view?id=<%= p.getOrderId() %>">Track order</a>
            <button class="btn plain block" type="button" onclick="window.print()">Print receipt</button>
        <% } else if (p.isExpired()) { %>
            <h2>Expired</h2>
            <p>This prescription can no longer be paid. Please upload a new one.</p>
            <a class="btn block" href="<%= ctx %>/prescriptions/upload">Upload a new prescription</a>
        <% } else { %>
            <% BigDecimal fee = OrderService.deliveryFeeFor(p.getTotal());
               BigDecimal toPay = OrderService.totalFor(p.getTotal()); %>
            <h2>Total to pay</h2>
            <div class="price big"><%= TextUtil.money(toPay) %></div>
            <div class="totals">
                <div><span>Medicines</span><span><%= TextUtil.money(p.getTotal()) %></span></div>
                <div><span>Delivery</span><span><%= fee.signum() == 0 ? "Free" : TextUtil.money(fee) %></span></div>
            </div>
            <a class="btn block pay-button" href="<%= ctx %>/prescriptions/pay?id=<%= p.getId() %>">
                Pay <%= TextUtil.money(toPay) %></a>
            <p class="meta center">Please pay before <%= TextUtil.date(p.getExpiresAt().toLocalDate()) %>.</p>
        <% } %>
    </aside>
    <% } %>
</div>

<%@ include file="../common/footer.jspf" %>
