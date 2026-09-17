<%--
    Test payment for an approved prescription.
    Filled by PrescriptionPaymentServlet (/prescriptions/pay?id=).

    The card check is a placeholder for module 02 (see PaymentService).
    No card details are stored, only the last 4 digits.

    Module : 05 - Prescription Upload and Verification
    Owner  : Perera D. A. A. N. S.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Prescription" %>
<%@ page import="com.medisys.model.PrescriptionItem" %>
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
%>
<%!
    private String val(Map<String, String> form, String field) {
        return com.medisys.util.TextUtil.html(form.get(field));
    }
%>

<nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="<%= ctx %>/prescriptions">My Prescriptions</a> <span aria-hidden="true">/</span>
    <a href="<%= ctx %>/prescriptions/view?id=<%= p.getId() %>"><%= p.getReference() %></a>
    <span aria-hidden="true">/</span> <span aria-current="page">Pay</span>
</nav>

<div class="view-layout">
    <section class="card">
        <h1>Payment</h1>
        <p class="test-note"><strong>Test payment.</strong> No money is taken. Use any valid-looking card,
            for example <code>4242 4242 4242 4242</code>, any future expiry date and any 3-digit CVV.
            Card details are never stored.</p>

        <% if (errors != null && !errors.isEmpty()) { %>
            <div class="message error" role="alert">
                <ul><% for (String e : errors) { %><li><%= TextUtil.html(e) %></li><% } %></ul>
            </div>
        <% } %>

        <form method="post" action="<%= ctx %>/prescriptions/pay" data-pay-form novalidate autocomplete="on">
            <input type="hidden" name="id" value="<%= p.getId() %>">

            <h2>Delivery</h2>
            <div class="row">
                <div class="field">
                    <label for="deliveryName">Name *</label>
                    <input type="text" id="deliveryName" name="deliveryName" value="<%= val(form, "deliveryName") %>"
                           maxlength="100" autocomplete="name" required>
                </div>
                <div class="field">
                    <label for="deliveryPhone">Contact number *</label>
                    <input type="tel" id="deliveryPhone" name="deliveryPhone" value="<%= val(form, "deliveryPhone") %>"
                           maxlength="15" autocomplete="tel" placeholder="0771234567" required>
                </div>
            </div>
            <div class="field">
                <label for="deliveryAddress">Delivery address *</label>
                <textarea id="deliveryAddress" name="deliveryAddress" rows="2" maxlength="255"
                          autocomplete="street-address" required><%= val(form, "deliveryAddress") %></textarea>
            </div>

            <h2>Card</h2>
            <div class="field">
                <label for="cardName">Name on card *</label>
                <input type="text" id="cardName" name="cardName" value="<%= val(form, "cardName") %>"
                       maxlength="100" autocomplete="cc-name" required>
            </div>
            <div class="field">
                <label for="cardNumber">Card number *</label>
                <input type="text" id="cardNumber" name="cardNumber" inputmode="numeric" maxlength="23"
                       autocomplete="cc-number" placeholder="1234 5678 9012 3456" required data-card-number>
            </div>
            <div class="row">
                <div class="field">
                    <label for="cardExpiry">Expiry (MM/YY) *</label>
                    <input type="text" id="cardExpiry" name="cardExpiry" value="<%= val(form, "cardExpiry") %>"
                           inputmode="numeric" maxlength="5" autocomplete="cc-exp" placeholder="MM/YY" required data-card-expiry>
                </div>
                <div class="field">
                    <label for="cardCvv">CVV *</label>
                    <input type="password" id="cardCvv" name="cardCvv" inputmode="numeric" maxlength="4"
                           autocomplete="cc-csc" placeholder="123" required>
                </div>
            </div>

            <button class="btn block pay-button" type="submit" data-busy-text="Processing payment...">
                Pay <%= TextUtil.money(p.getTotal()) %></button>
            <p class="meta center">By paying you confirm the delivery details above.</p>
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
            <div><span>Delivery</span><span>Free</span></div>
            <div class="grand"><span>Total</span><span><%= TextUtil.money(p.getTotal()) %></span></div>
        </div>
        <p class="meta">Prescription <%= p.getReference() %> &middot; approved by
            <%= TextUtil.html(p.getReviewedByName()) %></p>
    </aside>
</div>

<script src="<%= ctx %>/js/prescription.js" defer></script>
<%@ include file="../common/footer.jspf" %>
