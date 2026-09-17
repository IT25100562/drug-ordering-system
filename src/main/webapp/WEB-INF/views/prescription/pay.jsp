<%--
    Test payment for an approved prescription. Paying creates an order (module 02).
    Filled by PrescriptionPaymentServlet (/prescriptions/pay?id=).

    Module : 05 - Prescription Upload and Verification
    Owner  : Perera D. A. A. N. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Prescription" %>
<%@ page import="com.medisys.model.PrescriptionItem" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<% String pageTitle = "Pay for Prescription"; %>
<%@ include file="../common/header.jspf" %>
<%
    Prescription p = (Prescription) request.getAttribute("prescription");
    @SuppressWarnings("unchecked")
    Map<String, String> form = (Map<String, String>) request.getAttribute("form");
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
    BigDecimal deliveryFee = (BigDecimal) request.getAttribute("deliveryFee");
    BigDecimal total = (BigDecimal) request.getAttribute("total");
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/prescriptions">My Prescriptions</a> <span aria-hidden="true">/</span>
    <a href="<%= ctx %>/prescriptions/view?id=<%= p.getId() %>"><%= p.getReference() %></a>
    <span aria-hidden="true">/</span> <span aria-current="page">Pay</span>
</nav>

<div class="view-layout">
    <section class="card">
        <h1>Payment</h1>
        <form method="post" action="<%= ctx %>/prescriptions/pay" data-pay-form novalidate autocomplete="on">
            <input type="hidden" name="id" value="<%= p.getId() %>">
            <%@ include file="../common/checkout-fields.jspf" %>
        </form>
    </section>

    <aside class="card pay-box">
        <h2>Order summary</h2>
        <ul class="summary-lines">
            <% for (PrescriptionItem item : p.getItems()) { %>
                <li>
                    <span><%= TextUtil.html(item.getMedicine().getDisplayName()) %>
                        <span class="meta">&times; <%= item.getQuantity() %></span></span>
                    <span class="nowrap"><%= TextUtil.money(item.getLineTotal()) %></span>
                </li>
            <% } %>
        </ul>
        <div class="totals">
            <div><span>Subtotal</span><span><%= TextUtil.money(p.getTotal()) %></span></div>
            <div><span>Delivery</span><span><%= deliveryFee.signum() == 0 ? "Free" : TextUtil.money(deliveryFee) %></span></div>
            <div class="grand"><span>Total</span><span><%= TextUtil.money(total) %></span></div>
        </div>
        <p class="meta">Prescription <%= p.getReference() %> &middot; approved by
            <%= TextUtil.html(p.getReviewedByName()) %></p>
    </aside>
</div>

<%@ include file="../common/footer.jspf" %>
