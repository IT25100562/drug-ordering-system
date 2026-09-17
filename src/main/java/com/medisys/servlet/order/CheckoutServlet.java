package com.medisys.servlet.order;

import com.medisys.model.Cart;
import com.medisys.model.Order;
import com.medisys.model.User;
import com.medisys.service.CartService;
import com.medisys.service.OrderService;
import com.medisys.service.ValidationException;
import com.medisys.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checkout: order summary, delivery details and (test) card payment on one page.
 *
 *   GET  /checkout
 *   POST /checkout   delivery fields, card fields, expectedTotal
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/order/checkout.jsp";

    private final OrderService orderService = new OrderService();
    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            Cart cart = loadReadyCart(request, response, user);
            if (cart == null) {
                return;
            }
            showForm(request, response, cart, startForm(user), null);
        } catch (SQLException e) {
            throw new ServletException("Could not load the checkout", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        Map<String, String> form = readForm(request);
        try {
            try {
                Order order = orderService.placeCartOrder(user, form);
                // The order page shows a thank-you box for placed=1, so no flash message here.
                response.sendRedirect(request.getContextPath() + "/orders/view?id=" + order.getId() + "&placed=1");
            } catch (ValidationException e) {
                Cart cart = loadReadyCart(request, response, user);
                if (cart == null) {
                    return;
                }
                // Never show the card number or CVV again.
                form.remove("cardNumber");
                form.remove("cardCvv");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                showForm(request, response, cart, form, e.getErrors());
            }
        } catch (SQLException e) {
            throw new ServletException("Could not place the order", e);
        }
    }

    /** The cart if it can be checked out; otherwise back to /cart with a message and null. */
    private Cart loadReadyCart(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        Cart cart = cartService.getCart(user.getId());
        if (cart.isEmpty()) {
            SessionUtil.flash(request, "info", "Your cart is empty. Add some medicines first.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return null;
        }
        if (!cart.isReadyForCheckout()) {
            SessionUtil.flash(request, "error", "Some items in your cart need attention before you can check out.");
            response.sendRedirect(request.getContextPath() + "/cart");
            return null;
        }
        return cart;
    }

    /** The form starts with the customer's own details. */
    static Map<String, String> startForm(User user) {
        Map<String, String> form = new HashMap<>();
        form.put("deliveryName", user.getFullName());
        form.put("deliveryAddress", user.getAddress());
        form.put("deliveryPhone", user.getPhone());
        form.put("cardName", user.getFullName());
        return form;
    }

    static Map<String, String> readForm(HttpServletRequest request) {
        Map<String, String> form = new HashMap<>();
        for (String field : OrderService.FORM_FIELDS) {
            form.put(field, request.getParameter(field));
        }
        return form;
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Cart cart,
                          Map<String, String> form, List<String> errors) throws ServletException, IOException {
        request.setAttribute("cart", cart);
        request.setAttribute("deliveryFee", OrderService.deliveryFeeFor(cart.getSubtotal()));
        request.setAttribute("total", OrderService.totalFor(cart.getSubtotal()));
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
