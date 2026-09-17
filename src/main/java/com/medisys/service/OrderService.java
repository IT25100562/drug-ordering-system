package com.medisys.service;

import com.medisys.dao.OrderDAO;
import com.medisys.dao.StockShortageException;
import com.medisys.dao.impl.OrderDAOImpl;
import com.medisys.model.Cart;
import com.medisys.model.CartItem;
import com.medisys.model.Medicine;
import com.medisys.model.Order;
import com.medisys.model.OrderItem;
import com.medisys.model.OrderStatus;
import com.medisys.model.Payment;
import com.medisys.model.Prescription;
import com.medisys.model.PrescriptionItem;
import com.medisys.model.User;
import com.medisys.util.TextUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * All business rules of Order Placement and Checkout.
 *
 * Placing an order (customer)
 *  - from the cart: every line must be buyable (see Cart.isReadyForCheckout)
 *  - from an approved prescription: at the prices the pharmacist approved
 *  - delivery: Rs. 300, free from Rs. 2,500
 *  - the customer must see the same total that is charged (expectedTotal)
 *  - delivery details and the (test) card are checked; the order, payment and
 *    stock change are saved together or not at all
 *
 * After the order
 *  - the customer can cancel while it is "Order placed" (not yet packed)
 *  - the pharmacy marks it as packed; the rider then moves it on to
 *    out for delivery -> delivered (DeliveryService, module 06)
 *  - the pharmacy can cancel it until it leaves (a reason is required)
 *  - cancelling puts the stock back and refunds the payment
 *  - the customer is notified at every step
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 */
public class OrderService {

    public static final BigDecimal DELIVERY_FEE = new BigDecimal("300.00");
    public static final BigDecimal FREE_DELIVERY_FROM = new BigDecimal("2500.00");
    public static final int NOTE_MAX = 300;
    public static final int CANCEL_REASON_MIN = 5;

    /** The fields of the checkout form (common/checkout-fields.jspf). */
    public static final String[] FORM_FIELDS = {
            "deliveryName", "deliveryAddress", "deliveryPhone", "deliveryNote",
            "cardName", "cardNumber", "cardExpiry", "cardCvv", "expectedTotal"
    };

    private final OrderDAO orderDAO = new OrderDAOImpl();
    private final CartService cartService = new CartService();
    private final MedicineService medicineService = new MedicineService();
    private final PaymentService paymentService = new PaymentService();
    private final NotificationService notificationService = new NotificationService();

    /** Delivery fee for an order with this subtotal. */
    public static BigDecimal deliveryFeeFor(BigDecimal subtotal) {
        return subtotal.compareTo(FREE_DELIVERY_FROM) >= 0 ? BigDecimal.ZERO.setScale(2) : DELIVERY_FEE;
    }

    /** Subtotal + delivery fee. */
    public static BigDecimal totalFor(BigDecimal subtotal) {
        return subtotal.add(deliveryFeeFor(subtotal));
    }

    // ============================================================ placing

    /**
     * Checks out the customer's cart.
     *
     * @param form delivery fields (deliveryName, deliveryAddress, deliveryPhone, deliveryNote),
     *             card fields (see PaymentService.checkTestCard) and expectedTotal
     */
    public Order placeCartOrder(User customer, Map<String, String> form) throws SQLException, ValidationException {
        Cart cart = cartService.getCart(customer.getId());
        if (cart.isEmpty()) {
            throw new ValidationException("Your cart is empty.");
        }
        if (!cart.isReadyForCheckout()) {
            throw new ValidationException("Some items in your cart need attention. Please review your cart first.");
        }

        List<OrderItem> items = new ArrayList<>();
        for (CartItem line : cart.getItems()) {
            items.add(new OrderItem(line.getMedicine(), line.getMedicine().getPrice(), line.getQuantity(), null));
        }
        Order order = buildOrder(customer, Order.SOURCE_CART, items, cart.getSubtotal(), form);

        int id;
        try {
            id = orderDAO.create(order, null);
        } catch (StockShortageException e) {
            throw shortage(e, items);
        }
        Order placed = orderDAO.findById(id);
        notifyPlaced(placed);
        return placed;
    }

    /**
     * Pays for an approved prescription: creates an order with the medicines
     * and prices the pharmacist approved. Module 05 calls this.
     */
    public Order placePrescriptionOrder(User customer, Prescription p, Map<String, String> form)
            throws SQLException, ValidationException {
        List<OrderItem> items = new ArrayList<>();
        for (PrescriptionItem line : p.getItems()) {
            items.add(new OrderItem(line.getMedicine(), line.getUnitPrice(), line.getQuantity(),
                    line.getDosageInstructions()));
        }
        Order order = buildOrder(customer, Order.SOURCE_PRESCRIPTION, items, p.getTotal(), form);

        int id;
        try {
            id = orderDAO.create(order, p.getId());
        } catch (StockShortageException e) {
            throw shortage(e, items);
        }
        if (id == OrderDAO.NOT_PAYABLE) {
            throw new ValidationException(p.getReference() + " can no longer be paid. Please refresh the page.");
        }
        Order placed = orderDAO.findById(id);
        notifyPlaced(placed);
        return placed;
    }

    /** Checks the form and puts together the order (not saved yet). */
    private Order buildOrder(User customer, String source, List<OrderItem> items, BigDecimal subtotal,
                             Map<String, String> form) throws ValidationException {
        BigDecimal fee = deliveryFeeFor(subtotal);
        BigDecimal total = subtotal.add(fee);

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
        if (note.length() > NOTE_MAX) {
            errors.add("The delivery note can have at most " + NOTE_MAX + " characters.");
        }
        PaymentService.CardCheck card = paymentService.checkTestCard(form, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

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
        payment.setCardLast4(card.last4);
        payment.setReference(paymentService.newReference());
        order.setPayment(payment);
        return order;
    }

    private ValidationException shortage(StockShortageException e, List<OrderItem> items) throws SQLException {
        String name = "One of the medicines";
        for (OrderItem item : items) {
            if (item.getMedicineId() == e.getMedicineId()) {
                name = item.getMedicineName();
            }
        }
        Medicine medicine = medicineService.getMedicine(e.getMedicineId());
        String left = medicine == null || medicine.isDiscontinued() ? "is no longer available"
                : "has only " + medicine.getStockQuantity() + " left";
        return new ValidationException("Sorry, " + name + " " + left
                + ". Your order was not placed and your card was not charged.");
    }

    private void notifyPlaced(Order o) {
        notificationService.notify(o.getUserId(), "Thank you! Order " + o.getReference() + " ("
                + TextUtil.money(o.getTotal()) + ") was placed and paid. Payment reference "
                + o.getPayment().getReference() + ".", "/orders/view?id=" + o.getId());
        // The delivery (module 06) was already created in the same transaction
        // as the order (OrderDAOImpl.create), so it can't be missing.
    }

    // =========================================================== customer

    public List<Order> getMyOrders(int userId) throws SQLException {
        return orderDAO.findByUser(userId);
    }

    /** The customer's own order, or an error (also when it belongs to someone else). */
    public Order getOwnOrder(User customer, int id) throws SQLException, ValidationException {
        Order order = orderDAO.findById(id);
        if (order == null || order.getUserId() != customer.getId()) {
            throw new ValidationException("That order was not found.");
        }
        return order;
    }

    /** The customer cancels an order that has not been packed yet. */
    public Order cancelByCustomer(User customer, int id, String reasonText) throws SQLException, ValidationException {
        Order order = getOwnOrder(customer, id);
        if (!order.getStatus().canBeCancelledByCustomer()) {
            throw new ValidationException(order.getReference() + " can no longer be cancelled ("
                    + order.getStatus().getLabel() + "). Please contact the pharmacy.");
        }
        String reason = TextUtil.clean(reasonText);
        if (reason.length() > NOTE_MAX) {
            reason = reason.substring(0, NOTE_MAX);
        }
        String note = "Cancelled by the customer" + (reason.isEmpty() ? "" : ": " + reason);
        if (!orderDAO.cancel(id, EnumSet.of(OrderStatus.PAID), null, note)) {
            throw new ValidationException(order.getReference() + " is already being packed and can no longer "
                    + "be cancelled. Please contact the pharmacy.");
        }
        notificationService.notify(order.getUserId(), "Order " + order.getReference() + " was cancelled. "
                + "Your payment of " + TextUtil.money(order.getTotal()) + " has been refunded."
                + prescriptionNote(order), "/orders/view?id=" + id);
        return order;
    }

    /**
     * Puts the medicines of an old order into the cart again. Prescription-only
     * and unavailable medicines are skipped (with a message).
     */
    public String reorder(User customer, int id) throws SQLException, ValidationException {
        Order order = getOwnOrder(customer, id);
        int added = 0;
        List<String> skipped = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            try {
                cartService.addToCart(customer.getId(), item.getMedicineId(), String.valueOf(item.getQuantity()));
                added++;
            } catch (ValidationException e) {
                skipped.add(item.getMedicineName());
            }
        }
        if (added == 0) {
            throw new ValidationException("None of these medicines can be added to the cart right now"
                    + (order.isFromPrescription() ? " (prescription medicines need a new prescription)." : "."));
        }
        return added + " item" + (added == 1 ? " was" : "s were") + " added to your cart."
                + (skipped.isEmpty() ? "" : " Not added: " + String.join(", ", skipped) + ".");
    }

    // ============================================================== admin

    /** @param filter a status name, OrderDAO.FILTER_OPEN or OrderDAO.FILTER_ALL */
    public List<Order> getOrders(String filter, String keyword) throws SQLException {
        return orderDAO.findForAdmin(filter, TextUtil.clean(keyword));
    }

    public Map<String, Integer> getCounts() throws SQLException {
        return orderDAO.countByStatus();
    }

    public Order getOrder(int id) throws SQLException {
        return orderDAO.findById(id);
    }

    /**
     * Marks a new order as packed ("Being packed"). The later steps are done
     * by the rider through DeliveryService.
     *
     * @param currentText the status the admin saw on the page - stops a double
     *                    click from moving the order two steps
     */
    public String advance(User admin, int id, String currentText, String noteText)
            throws SQLException, ValidationException {
        Order order = orderDAO.findById(id);
        if (order == null) {
            throw new ValidationException("That order no longer exists.");
        }
        OrderStatus seen = OrderStatus.fromText(currentText);
        if (seen != order.getStatus()) {
            throw new ValidationException(order.getReference() + " was already changed to \""
                    + order.getStatus().getLabel() + "\". Please check it again.");
        }
        OrderStatus next = order.getStatus().nextForPharmacy();
        if (next == null) {
            throw new ValidationException(order.getReference() + " is " + order.getStatus().getLabel()
                    + " and cannot be moved on here. Delivery staff update it from the Deliveries page.");
        }
        String note = TextUtil.clean(noteText);
        if (note.length() > NOTE_MAX) {
            throw new ValidationException("The note can have at most " + NOTE_MAX + " characters.");
        }
        if (!orderDAO.advance(id, order.getStatus(), next, admin.getId(), note.isEmpty() ? null : note)) {
            throw new ValidationException(order.getReference() + " was just changed by someone else.");
        }

        String message = "Good news: order " + order.getReference() + " is being packed.";
        if (!note.isEmpty()) {
            message += " Note: " + note;
        }
        notificationService.notify(order.getUserId(), message, "/orders/view?id=" + id);
        return order.getReference() + " is now \"" + next.getLabel() + "\". The customer has been notified.";
    }

    /** The pharmacy cancels an order that has not left yet. A reason is required. */
    public String cancelByAdmin(User admin, int id, String reasonText) throws SQLException, ValidationException {
        Order order = orderDAO.findById(id);
        if (order == null) {
            throw new ValidationException("That order no longer exists.");
        }
        if (!order.getStatus().canBeCancelledByPharmacy()) {
            throw new ValidationException(order.getReference() + " is " + order.getStatus().getLabel()
                    + " and can no longer be cancelled.");
        }
        String reason = TextUtil.clean(reasonText);
        if (reason.length() < CANCEL_REASON_MIN || reason.length() > NOTE_MAX) {
            throw new ValidationException("Please write a reason for the customer (" + CANCEL_REASON_MIN
                    + " to " + NOTE_MAX + " characters).");
        }
        if (!orderDAO.cancel(id, EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING), admin.getId(), reason)) {
            throw new ValidationException(order.getReference() + " was just changed by someone else.");
        }
        notificationService.notify(order.getUserId(), "Sorry, order " + order.getReference()
                + " was cancelled by the pharmacy: " + withFullStop(reason) + " Your payment of "
                + TextUtil.money(order.getTotal()) + " has been refunded." + prescriptionNote(order),
                "/orders/view?id=" + id);
        return order.getReference() + " was cancelled, the stock was put back and the payment refunded.";
    }

    /** "Out of stock" -> "Out of stock." (so the next sentence reads well). */
    private static String withFullStop(String text) {
        return text.matches(".*[.!?]$") ? text : text + ".";
    }

    private String prescriptionNote(Order order) {
        return order.isFromPrescription()
                ? " You can pay for prescription " + order.getPrescriptionReference() + " again while it is valid."
                : "";
    }
}
