package com.medisys.order;

import com.medisys.cart.Cart;
import com.medisys.cart.CartDAO;
import com.medisys.cart.CartItem;
import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.ValidationException;
import com.medisys.common.Validator;
import com.medisys.delivery.NotificationDAO;
import com.medisys.medicine.Medicine;
import com.medisys.medicine.MedicineDAO;
import com.medisys.user.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checkout: the customer pays for the cart and the order is placed (CREATE an order).
 *
 *   GET  /checkout    delivery details + test card form, with the totals
 *   POST /checkout    validate, take the payment, save the order
 *
 * Rules:
 *  - every cart line must be buyable (see CartItem.getProblem)
 *  - delivery Rs. 300, free from Rs. 2,500 (Order.deliveryFeeFor)
 *  - the customer must see the same total that is charged (expectedTotal)
 *  - the order, payment and stock change are saved together or not at all
 *    (OrderDAO.placeOrder runs one transaction)
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/order/checkout.jsp";

    /** The fields of the checkout form (common/checkout-fields.jspf). */
    public static final String[] FORM_FIELDS = {
            "deliveryName", "deliveryAddress", "deliveryPhone", "deliveryNote",
            "cardName", "cardNumber", "cardExpiry", "cardCvv", "expectedTotal"
    };

    private static final OrderDAO orderDAO = new OrderDAO();
    private static final MedicineDAO medicineDAO = new MedicineDAO();
    private static final NotificationDAO notificationDAO = new NotificationDAO();
    private final CartDAO cartDAO = new CartDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            Cart cart = loadReadyCart(request, response, user);
            if (cart != null) {
                showForm(request, response, cart, startForm(user), null);
            }
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
            Cart cart = loadReadyCart(request, response, user);
            if (cart == null) {
                return;
            }
            List<OrderItem> items = new ArrayList<>();
            for (CartItem line : cart.getItems()) {
                items.add(new OrderItem(line.getMedicine(), line.getMedicine().getPrice(), line.getQuantity(), null));
            }
            try {
                Order order = placeOrder(user, Order.SOURCE_CART, items, cart.getSubtotal(), null, form);
                // The order page shows a thank-you box for placed=1, so no flash message here.
                response.sendRedirect(request.getContextPath() + "/orders/view?id=" + order.getId() + "&placed=1");
            } catch (ValidationException e) {
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

    // ================================================================ CREATE

    /**
     * Checks the form, takes the (test) payment and saves the order.
     * The cart checkout and the prescription payment (module 05) both use this.
     *
     * @param prescriptionId the prescription being paid, or null for a cart order
     * @param form           delivery fields, card fields and expectedTotal
     */
    public static Order placeOrder(User customer, String source, List<OrderItem> items, BigDecimal subtotal,
                                   Integer prescriptionId, Map<String, String> form)
            throws SQLException, ValidationException {
        BigDecimal fee = Order.deliveryFeeFor(subtotal);
        BigDecimal total = subtotal.add(fee);

        // ---- validation
        // The page shows the total it was built with. If the cart or prices
        // changed in the meantime, the customer must look again before paying.
        BigDecimal expected = TextUtil.parseDecimal(form.get("expectedTotal"));
        if (expected == null || expected.compareTo(total) != 0) {
            throw new ValidationException("The total changed while you were checking out. "
                    + "Please check the new total of " + TextUtil.money(total) + " and pay again.");
        }
        List<String> errors = new ArrayList<>();
        String name = TextUtil.clean(form.get("deliveryName"));
        String address = TextUtil.clean(form.get("deliveryAddress"));
        String phone = TextUtil.clean(form.get("deliveryPhone"));
        String note = TextUtil.clean(form.get("deliveryNote"));
        if (name.length() < 2 || name.length() > 100) {
            errors.add("Please enter the name of the person receiving the medicines.");
        }
        if (address.length() < 5 || address.length() > 255) {
            errors.add("Please enter the full delivery address.");
        }
        if (!phone.matches("\\+?[0-9 ]{9,15}")) {
            errors.add("Please enter a valid contact number, e.g. 0771234567.");
        }
        if (note.length() > 300) {
            errors.add("The delivery note can have at most 300 characters.");
        }
        String cardLast4 = Validator.card(form, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // ---- build the order and its payment
        Order order = new Order();
        order.setUserId(customer.getId());
        order.setSource(source);
        order.setItems(items);
        order.setSubtotal(subtotal);
        order.setDeliveryFee(fee);
        order.setTotal(total);
        order.setDeliveryName(name);
        order.setDeliveryAddress(address);
        order.setDeliveryPhone(phone);
        order.setDeliveryNote(note.isEmpty() ? null : note);
        Payment payment = new Payment();
        payment.setAmount(total);
        payment.setCardLast4(cardLast4);
        payment.setReference(Payment.newReference());
        order.setPayment(payment);

        // ---- save (one transaction)
        int id;
        try {
            id = orderDAO.placeOrder(order, prescriptionId);
        } catch (OrderDAO.StockShortageException e) {
            Medicine medicine = medicineDAO.getMedicineById(e.getMedicineId());
            String medicineName = "One of the medicines";
            for (OrderItem item : items) {
                if (item.getMedicineId() == e.getMedicineId()) {
                    medicineName = item.getMedicineName();
                }
            }
            String left = medicine == null || medicine.isDiscontinued() ? "is no longer available"
                    : "has only " + medicine.getStockQuantity() + " left";
            throw new ValidationException("Sorry, " + medicineName + " " + left
                    + ". Your order was not placed and your card was not charged.");
        }
        if (id == OrderDAO.NOT_PAYABLE) {
            throw new ValidationException(String.format("RX-%06d", prescriptionId)
                    + " can no longer be paid. Please refresh the page.");
        }

        Order placed = orderDAO.getOrderById(id);
        notificationDAO.addNotification(placed.getUserId(), "Thank you! Order " + placed.getReference() + " ("
                + TextUtil.money(placed.getTotal()) + ") was placed and paid. Payment reference "
                + placed.getPayment().getReference() + ".", "/orders/view?id=" + placed.getId());
        return placed;
    }

    // =============================================================== helpers

    /** The cart if it can be checked out; otherwise back to /cart with a message, and null. */
    private Cart loadReadyCart(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        Cart cart = cartDAO.getCart(user.getId());
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
    public static Map<String, String> startForm(User user) {
        Map<String, String> form = new HashMap<>();
        form.put("deliveryName", user.getFullName());
        form.put("deliveryAddress", user.getAddress());
        form.put("deliveryPhone", user.getPhone());
        form.put("cardName", user.getFullName());
        return form;
    }

    public static Map<String, String> readForm(HttpServletRequest request) {
        Map<String, String> form = new HashMap<>();
        for (String field : FORM_FIELDS) {
            form.put(field, request.getParameter(field));
        }
        return form;
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Cart cart,
                          Map<String, String> form, List<String> errors) throws ServletException, IOException {
        request.setAttribute("cart", cart);
        request.setAttribute("deliveryFee", Order.deliveryFeeFor(cart.getSubtotal()));
        request.setAttribute("total", Order.totalFor(cart.getSubtotal()));
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
