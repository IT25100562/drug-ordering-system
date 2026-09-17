<%--
    Checkout: order summary, delivery details and test card payment.
    Filled by CheckoutServlet (/checkout).

    Module : 02 - Order Placement and Checkout
    Owner  : Hewage B. H. A. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Cart" %>
<%@ page import="com.medisys.model.CartItem" %>
<%@ page import="com.medisys.service.OrderService" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<% String pageTitle = "Checkout"; %>
<%@ include file="../common/header.jspf" %>
<%
    Cart cart = (Cart) request.getAttribute("cart");
    @SuppressWarnings("unchecked")
    Map<String, String> form = (Map<String, String>) request.getAttribute("form");
    @SuppressWarnings("unchecked")
    List<String> errors = (List<String>) request.getAttribute("errors");
    BigDecimal deliveryFee = (BigDecimal) request.getAttribute("deliveryFee");
    BigDecimal total = (BigDecimal) request.getAttribute("total");
    BigDecimal missingForFree = OrderService.FREE_DELIVERY_FROM.subtract(cart.getSubtotal());
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/cart">My Cart</a> <span aria-hidden="true">/</span>
    <span aria-current="page">Checkout</span>
</nav>

<ol class="checkout-steps" aria-label="Checkout steps">
    <li class="done">Cart</li>
    <li class="current">Delivery &amp; payment</li>
    <li>Confirmation</li>
</ol>

<div class="view-layout">
    <section class="card">
        <h1>Checkout</h1>
        <form method="post" action="<%= ctx %>/checkout" data-pay-form novalidate autocomplete="on">
            <%@ include file="../common/checkout-fields.jspf" %>
        </form>
    </section>

    <aside class="card pay-box">
        <div class="title-row">
            <h2>Order summary</h2>
            <a class="meta" href="<%= ctx %>/cart">Edit cart</a>
        </div>
        <ul class="summary-lines">
            <% for (CartItem item : cart.getItems()) { %>
                <li>
                    <span><%= TextUtil.html(item.getMedicine().getDisplayName()) %>
                        <span class="meta">&times; <%= item.getQuantity() %></span></span>
                    <span class="nowrap"><%= TextUtil.money(item.getLineTotal()) %></span>
                </li>
            <% } %>
        </ul>
        <div class="totals">
            <div><span>Subtotal (<%= cart.getItemCount() %> item<%= cart.getItemCount() == 1 ? "" : "s" %>)</span>
                <span><%= TextUtil.money(cart.getSubtotal()) %></span></div>
            <div><span>Delivery</span>
                <span><%= deliveryFee.signum() == 0 ? "Free" : TextUtil.money(deliveryFee) %></span></div>
            <div class="grand"><span>Total</span><span><%= TextUtil.money(total) %></span></div>
        </div>
        <% if (missingForFree.signum() > 0) { %>
            <p class="free-delivery-hint">Add <%= TextUtil.money(missingForFree) %> more for free delivery.</p>
        <% } else { %>
            <p class="free-delivery-hint done">&#10003; You get free delivery.</p>
        <% } %>
        <p class="meta">Delivered within 1-2 working days. You can cancel until we start packing.</p>
    </aside>
</div>

<%@ include file="../common/footer.jspf" %>
