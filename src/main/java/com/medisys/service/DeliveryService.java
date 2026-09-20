package com.medisys.service;

import com.medisys.dao.DeliveryDAO;
import com.medisys.dao.impl.DeliveryDAOImpl;
import com.medisys.model.Delivery;
import com.medisys.model.DeliveryStatus;
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
 *  - the staff page has three tabs: New (at the pharmacy), On the way, Completed
 *  - riders work directly with new parcels, with no admin step in between:
 *    any rider takes a new parcel with "Got the package". It becomes theirs and
 *    goes straight to "Out for delivery" (the order is marked packed and shipped).
 *  - on the way: delivered / could not deliver, and try again after a failed attempt
 *  - "could not deliver" needs a reason for the customer
 *  - only riders change deliveries. A rider sees their own deliveries plus the
 *    new ones nobody has taken yet; the admin can see all of them.
 *  - a customer can only track their own orders
 *  - the customer is notified at every step
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

    /** @param filter DeliveryDAO.FILTER_NEW, FILTER_ON_THE_WAY or FILTER_COMPLETED */
    public List<Delivery> getDeliveries(User user, String filter) throws SQLException {
        return deliveryDAO.findForStaff(onlyFor(user), filter);
    }

    public Map<String, Integer> getCounts(User user) throws SQLException {
        return deliveryDAO.countByStatus(onlyFor(user));
    }

    /**
     * A delivery the user may work on (with its updates), or null.
     * A rider may open their own deliveries and new ones nobody has taken yet.
     */
    public Delivery getForStaff(User user, int id) throws SQLException {
        Delivery delivery = deliveryDAO.findById(id);
        if (delivery == null) {
            return null;
        }
        boolean free = !delivery.hasRider() && delivery.getStatus() == DeliveryStatus.PENDING;
        if (!user.isAdmin() && !free && !Integer.valueOf(user.getId()).equals(delivery.getStaffId())) {
            return null;
        }
        return delivery;
    }

    /** Deliveries are the riders' job: the admin can look, but not change them. */
    private void ridersOnly(User user) throws ValidationException {
        if (user.isAdmin()) {
            throw new ValidationException("Only the rider can update a delivery.");
        }
    }

    /**
     * "Got the package": a rider takes a new parcel from the pharmacy.
     * It becomes theirs and is out for delivery straight away.
     *
     * @param currentText the status the rider saw on the page (stops double clicks)
     */
    public String pickUp(User rider, int id, String currentText) throws SQLException, ValidationException {
        ridersOnly(rider);
        Delivery delivery = getForStaff(rider, id);
        if (delivery == null) {
            throw new ValidationException("That delivery was not found, or another rider has taken it.");
        }
        String ref = delivery.getOrderReference();
        if (DeliveryStatus.fromText(currentText) != delivery.getStatus()
                || delivery.getStatus() != DeliveryStatus.PENDING) {
            throw new ValidationException(ref + " is already \"" + delivery.getStatus().getLabel()
                    + "\". Please check it again.");
        }

        if (!deliveryDAO.pickUp(delivery, rider.getId(), rider.getFullName(), rider.getId())) {
            throw new ValidationException(ref + " was just taken by another rider or cancelled. Please check it again.");
        }

        notificationService.notify(delivery.getCustomerId(), rider.getFullName() + " picked up your order " + ref
                + " from the pharmacy and is on the way to you. Please keep your phone nearby.",
                "/deliveries/track?orderId=" + delivery.getOrderId());
        return ref + " is now on the way. The customer has been notified.";
    }

    /**
     * The rider moves a delivery that is on the way to its next status.
     *
     * @param currentText the status the user saw on the page - stops a double
     *                    click from moving the delivery two steps
     * @param nextText    the status to move to (must be one of nextSteps())
     * @param noteText    optional note; for "could not deliver" the required reason
     */
    public String updateStatus(User user, int id, String currentText, String nextText, String noteText)
            throws SQLException, ValidationException {
        ridersOnly(user);
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
        if (delivery.getStatus() == DeliveryStatus.PENDING || !delivery.hasRider()) {
            // Leaving the pharmacy always goes through pickUp ("Got the package").
            throw new ValidationException("Please press \"Got the package\" for " + ref + " first.");
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
