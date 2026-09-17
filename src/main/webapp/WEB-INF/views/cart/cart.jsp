<%--
    The customer's shopping cart.
    Filled by CartServlet (/cart).

    Every button works as a normal form. cart.js makes the quantity, remove
    and save-for-later buttons update the page without reloading.

    Module : 01 - Shopping Cart and Wishlist
    Owner  : Amadini G. G. A.
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.medisys.model.Cart" %>
<%@ page import="com.medisys.model.CartItem" %>
<%@ page import="com.medisys.model.Medicine" %>
<% String pageTitle = "My Cart"; %>
<%@ include file="../common/header.jspf" %>
<%
    Cart cart = (Cart) request.getAttribute("cart");
    int wishlistCount = (Integer) request.getAttribute("wishlistCount");
    int maxPerItem = (Integer) request.getAttribute("maxPerItem");
%>

<div class="title-row">
    <div>
        <h1>My Cart</h1>
        <p class="subtitle" data-cart-summary-text>
            <%= cart.isEmpty() ? "Your cart is empty."
                    : cart.getItemCount() + " item" + (cart.getItemCount() == 1 ? "" : "s") + " in your cart" %>
        </p>
    </div>
    <a class="btn plain" href="<%= ctx %>/medicines">&larr; Continue shopping</a>
</div>

<div class="card empty-state <%= cart.isEmpty() ? "" : "hidden" %>" data-cart-empty>
    <h2>Your cart is empty</h2>
    <p>Browse our medicines and add what you need.</p>
    <a class="btn" href="<%= ctx %>/medicines">Browse medicines</a>
    <% if (wishlistCount > 0) { %>
        <p class="meta">You have <a href="<%= ctx %>/wishlist"><%= wishlistCount %> saved
            item<%= wishlistCount == 1 ? "" : "s" %></a> in your wishlist.</p>
    <% } %>
</div>

<% if (!cart.isEmpty()) { %>
<div class="cart-layout" data-cart>
    <%-- --------------------------------------------------------- lines --%>
    <section class="card cart-lines" aria-label="Items in your cart">
        <% for (CartItem item : cart.getItems()) {
               Medicine m = item.getMedicine();
               int maxAllowed = Math.max(1, item.getMaxQuantity());
        %>
        <div class="cart-line <%= item.hasProblem() ? "has-problem" : "" %>" data-line="<%= m.getId() %>">
            <a class="<%= m.getThumbCssClass() %>" href="<%= ctx %>/medicines/view?id=<%= m.getId() %>"
               tabindex="-1" aria-hidden="true"><%= m.getThumbText() %></a>

            <div class="line-info">
                <a class="line-name" href="<%= ctx %>/medicines/view?id=<%= m.getId() %>"><%= TextUtil.html(m.getDisplayName()) %></a>
                <span class="meta"><%= TextUtil.html(m.getCategoryName()) %> &middot; <%= TextUtil.html(m.getDosageForm()) %>
                    &middot; <%= TextUtil.money(m.getPrice()) %> each</span>
                <% if (m.isRequiresPrescription()) { %><span class="badge badge-rx">Rx</span><% } %>
                <p class="line-problem <%= item.hasProblem() ? "" : "hidden" %>" role="alert" data-problem>
                    <%= item.hasProblem() ? TextUtil.html(item.getProblem()) : "" %></p>

                <div class="line-links">
                    <form method="post" action="<%= ctx %>/wishlist/action" data-ajax="save-for-later">
                        <input type="hidden" name="medicineId" value="<%= m.getId() %>">
                        <input type="hidden" name="action" value="save-for-later">
                        <button class="link-button" type="submit">Save for later</button>
                    </form>
                    <form method="post" action="<%= ctx %>/cart/remove" data-ajax="cart-remove">
                        <input type="hidden" name="medicineId" value="<%= m.getId() %>">
                        <button class="link-button danger" type="submit">Remove</button>
                    </form>
                </div>
            </div>

            <form class="line-qty" method="post" action="<%= ctx %>/cart/update" data-ajax="cart-update">
                <input type="hidden" name="medicineId" value="<%= m.getId() %>">
                <label class="sr-only" for="qty-<%= m.getId() %>">Quantity of <%= TextUtil.html(m.getDisplayName()) %></label>
                <div class="stepper" data-stepper>
                    <button type="button" data-step="-1" aria-label="Decrease quantity">&minus;</button>
                    <input type="number" id="qty-<%= m.getId() %>" name="quantity" value="<%= item.getQuantity() %>"
                           min="1" max="<%= maxAllowed %>" step="1" inputmode="numeric"
                           data-last="<%= item.getQuantity() %>">
                    <button type="button" data-step="1" aria-label="Increase quantity">+</button>
                </div>
                <noscript><button class="btn small plain" type="submit">Update</button></noscript>
            </form>

            <div class="line-total" data-line-total><%= TextUtil.money(item.getLineTotal()) %></div>
        </div>
        <% } %>

        <form class="clear-cart" method="post" action="<%= ctx %>/cart/remove"
              data-confirm="Remove every item from your cart?">
            <input type="hidden" name="all" value="true">
            <button class="link-button danger" type="submit">Empty cart</button>
        </form>
    </section>

    <%-- ------------------------------------------------------- summary --%>
    <aside class="card cart-summary" aria-label="Order summary">
        <h2>Order summary</h2>
        <div class="totals">
            <div><span>Items</span><span data-item-count><%= cart.getItemCount() %></span></div>
            <div><span>Subtotal</span><span data-subtotal><%= TextUtil.money(cart.getSubtotal()) %></span></div>
            <div><span>Delivery</span><span class="meta">Calculated at checkout</span></div>
            <div class="grand"><span>Total so far</span><span data-subtotal><%= TextUtil.money(cart.getSubtotal()) %></span></div>
        </div>

        <p class="message error <%= cart.getProblemCount() == 0 ? "hidden" : "" %>" data-checkout-blocked>
            Please fix the highlighted item<%= cart.getProblemCount() == 1 ? "" : "s" %> before checking out.</p>

        <%-- Checkout is module 02 (Order Placement and Checkout). --%>
        <a class="btn block <%= cart.isReadyForCheckout() ? "" : "disabled" %>" href="<%= ctx %>/checkout"
           data-checkout <%= cart.isReadyForCheckout() ? "" : "aria-disabled=\"true\" tabindex=\"-1\"" %>>
            Proceed to checkout</a>
        <p class="meta center">Free delivery on orders of <%= TextUtil.money(com.medisys.service.OrderService.FREE_DELIVERY_FROM) %>
            or more (otherwise <%= TextUtil.money(com.medisys.service.OrderService.DELIVERY_FEE) %>).<br>
            Maximum <%= maxPerItem %> packs of each medicine per order.</p>
    </aside>
</div>
<% } %>

<script src="<%= ctx %>/js/cart.js" defer></script>
<%@ include file="../common/footer.jspf" %>
