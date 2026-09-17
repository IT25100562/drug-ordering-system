package com.medisys.service;

import com.medisys.dao.DeliveryDAO;
import com.medisys.dao.impl.DeliveryDAOImpl;
import com.medisys.model.Delivery;
import com.medisys.model.DeliveryStatus;
import com.medisys.model.OrderStatus;
import com.medisys.model.User;
import com.medisys.util.TextUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * All business rules of Delivery Tracking.
 *
 *  - every paid order gets a delivery automatically (in the order's transaction,
 *    see OrderDAOImpl.create); cancelling the order cancels it
 *  - the admin assigns a rider (delivery staff) while the parcel is at the pharmacy
 *  - the rider moves it on one step at a time:
 *      picked up (order must be packed) -> on the way -> delivered / could not deliver
 *    and tries again after a failed attempt
 *  - "could not deliver" needs a reason for the customer
 *  - a rider only sees and changes the deliveries given to them; the admin sees all
 *  - a customer can only track their own orders
 *  - the customer is notified at every step, the rider when a delivery is given to them
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
public class DeliveryService {

    public static final int NOTE_MAX = 300;
    public static final int REASON_MIN = 5;

    private final DeliveryDAO deliveryDAO = new DeliveryDAOImpl();
    private final NotificationService notificationService = new NotificationService();

    // =========================================================== customer

    /** The delivery of the customer's own order, or an error (also for someone else's order). */
    public Delivery getForCustomer(User customer, int orderId) throws SQLException, ValidationException {
        Delivery delivery = deliveryDAO.findByOrder(orderId);
        if (delivery == null || delivery.getCustomerId() != customer.getId()) {
            throw new ValidationException("That delivery was not found.");
        }
        return delivery;
    }

    /** The delivery of any order (for the admin's order page), or null. */
    public Delivery getForOrder(int orderId) throws SQLException {
        return deliveryDAO.findByOrder(orderId);
    }

    // ============================================================== staff

    /** The admin sees every delivery, a rider only their own. */
    private Integer onlyFor(User user) {
        return user.isAdmin() ? null : user.getId();
    }

    /** @param filter a status name or one of the DeliveryDAO.FILTER_ values */
    public List<Delivery> getDeliveries(User user, String filter) throws SQLException {
        return deliveryDAO.findForStaff(onlyFor(user), filter);
    }

    public Map<String, Integer> getCounts(User user) throws SQLException {
        return deliveryDAO.countByStatus(onlyFor(user));
    }

    /** A delivery the user may work on (with its updates), or null. */
    public Delivery getForStaff(User user, int id) throws SQLException {
        Delivery delivery = deliveryDAO.findById(id);
        if (delivery == null) {
            return null;
        }
        if (!user.isAdmin() && !Integer.valueOf(user.getId()).equals(delivery.getStaffId())) {
            return null;
        }
        return delivery;
    }

    public List<User> getRiders() throws SQLException {
        return deliveryDAO.findRiders();
    }

    /** The admin gives a delivery to a rider. */
    public String assign(User admin, int id, String riderText) throws SQLException, ValidationException {
        if (!admin.isAdmin()) {
            throw new ValidationException("Only an administrator can assign riders.");
        }
        Delivery delivery = deliveryDAO.findById(id);
        if (delivery == null) {
            throw new ValidationException("That delivery no longer exists.");
        }
        if (!delivery.getStatus().canAssignRider()) {
            throw new ValidationException(delivery.getOrderReference() + " is \"" + delivery.getStatus().getLabel()
                    + "\", so the rider can no longer be changed.");
        }

        Integer riderId = TextUtil.parseInt(riderText);
        User rider = null;
        for (User r : deliveryDAO.findRiders()) {
            if (riderId != null && r.getId() == riderId) {
                rider = r;
            }
        }
        if (rider == null) {
            throw new ValidationException("Please choose a rider from the list.");
        }
        if (riderId.equals(delivery.getStaffId())) {
            throw new ValidationException(delivery.getOrderReference() + " is already given to "
                    + rider.getFullName() + ".");
        }

        String note = "Rider: " + rider.getFullName() + (rider.getPhone() == null ? "" : ", " + rider.getPhone());
        if (!deliveryDAO.assignStaff(id, rider.getId(), admin.getId(), note)) {
            throw new ValidationException(delivery.getOrderReference() + " was just changed by someone else.");
        }

        notificationService.notify(rider.getId(), "New delivery for you: " + delivery.getOrderReference()
                + " to " + delivery.getDeliveryAddress() + ". Expected by "
                + TextUtil.date(delivery.getEstimatedDate()) + ".", "/staff/deliveries/view?id=" + id);
        notificationService.notify(delivery.getCustomerId(), rider.getFullName()
                + " will deliver your order " + delivery.getOrderReference() + ".",
                "/deliveries/track?orderId=" + delivery.getOrderId());
        return delivery.getOrderReference() + " was given to " + rider.getFullName() + ". The rider has been notified.";
    }

    /**
     * The rider (or the admin) moves a delivery to its next status.
     *
     * @param currentText the status the user saw on the page - stops a double
     *                    click from moving the delivery two steps
     * @param nextText    the status to move to (must be one of nextSteps())
     * @param noteText    optional note; for "could not deliver" the required reason
     */
    public String updateStatus(User user, int id, String currentText, String nextText, String noteText)
            throws SQLException, ValidationException {
        Delivery delivery = getForStaff(user, id);
        if (delivery == null) {
            throw new ValidationException("That delivery was not found.");
        }
        String ref = delivery.getOrderReference();

        DeliveryStatus seen = DeliveryStatus.fromText(currentText);
        if (seen != delivery.getStatus()) {
            throw new ValidationException(ref + " was already changed to \"" + delivery.getStatus().getLabel()
                    + "\". Please check it again.");
        }
        DeliveryStatus next = DeliveryStatus.fromText(nextText);
        if (next == null || !delivery.getStatus().nextSteps().contains(next)) {
            throw new ValidationException(ref + " is \"" + delivery.getStatus().getLabel()
                    + "\" and cannot be moved to that step.");
        }
        if (!delivery.hasRider()) {
            throw new ValidationException("Please assign a rider to " + ref + " first.");
        }
        if (next == DeliveryStatus.DISPATCHED && delivery.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new ValidationException(ref + " is not packed yet (the order is \""
                    + delivery.getOrderStatus().getLabel() + "\"). Please wait for the pharmacy.");
        }

        String note = TextUtil.clean(noteText);
        if (next == DeliveryStatus.FAILED && note.length() < REASON_MIN) {
            throw new ValidationException("Please write why the parcel could not be delivered (at least "
                    + REASON_MIN + " characters), e.g. \"Nobody at home\".");
        }
        if (note.length() > NOTE_MAX) {
            throw new ValidationException("The note can have at most " + NOTE_MAX + " characters.");
        }

        if (!deliveryDAO.updateStatus(delivery, delivery.getStatus(), next, user.getId(),
                note.isEmpty() ? null : note)) {
            throw new ValidationException(ref + " was just changed by someone else. Please check it again.");
        }

        notificationService.notify(delivery.getCustomerId(), customerMessage(delivery, next, note),
                "/deliveries/track?orderId=" + delivery.getOrderId());
        return ref + " is now \"" + next.getLabel() + "\". The customer has been notified.";
    }

    /** What the customer is told when their delivery reaches a status. */
    private String customerMessage(Delivery d, DeliveryStatus next, String note) {
        String ref = d.getOrderReference();
        String message;
        switch (next) {
            case DISPATCHED:
                message = "Your order " + ref + " has left the pharmacy with " + d.getStaffName() + ".";
                break;
            case OUT_FOR_DELIVERY:
                message = d.getAttempts() > 0
                        ? d.getStaffName() + " is trying again to deliver your order " + ref + " today."
                        : d.getStaffName() + " is on the way to you with order " + ref + ".";
                message += " Please keep your phone nearby.";
                break;
            case DELIVERED:
                message = "Order " + ref + " was delivered. Thank you for shopping with MediSys!";
                break;
            default:
                // FAILED: the note is the reason ("Nobody at home" -> "Nobody at home.")
                String reason = note.matches(".*[.!?]$") ? note : note + ".";
                return "We could not deliver order " + ref + ": " + reason + " Our rider will try again soon.";
        }
        return note.isEmpty() ? message : message + " Note: " + note;
    }
}
