package com.medisys.delivery;

import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.ValidationException;
import com.medisys.order.OrderDAO;
import com.medisys.user.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * The riders' delivery pages (the admin can look, but not change).
 *
 *   GET  /staff/deliveries?tab=NEW|ON_THE_WAY|COMPLETED     the list                   READ
 *   GET  /staff/deliveries/view?id=5                        one delivery               READ
 *   POST /staff/deliveries/update  action=pickup            "Got the package"          UPDATE
 *   POST /staff/deliveries/update  action=status, next=...  delivered / could not deliver / try again
 *
 * Rules:
 *  - every paid order gets a delivery automatically; cancelling the order cancels it
 *  - any rider takes a new parcel with "Got the package". It becomes theirs and
 *    goes straight to "Out for delivery" (the order is marked packed and shipped)
 *  - "could not deliver" needs a reason for the customer
 *  - a rider sees their own deliveries plus the new ones nobody has taken yet
 *  - the customer is notified at every step
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
@WebServlet({"/staff/deliveries", "/staff/deliveries/view", "/staff/deliveries/update"})
public class DeliveryServlet extends HttpServlet {

    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    // ================================================================== READ

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            if (request.getServletPath().equals("/staff/deliveries/view")) {
                Integer id = TextUtil.parseInt(request.getParameter("id"));
                Delivery delivery = id == null ? null : findForStaff(user, id);
                if (delivery == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                request.setAttribute("delivery", delivery);
                // The medicines to hand over (from module 02's order).
                request.setAttribute("order", orderDAO.getOrderById(delivery.getOrderId()));
                request.getRequestDispatcher("/WEB-INF/views/delivery/delivery-details.jsp").forward(request, response);
                return;
            }

            String filter = TextUtil.clean(request.getParameter("tab")).toUpperCase();
            if (!List.of(DeliveryDAO.FILTER_ON_THE_WAY, DeliveryDAO.FILTER_COMPLETED).contains(filter)) {
                filter = DeliveryDAO.FILTER_NEW;
            }
            // The admin sees every delivery, a rider only their own (+ new ones).
            Integer onlyFor = user.isAdmin() ? null : user.getId();
            request.setAttribute("deliveries", deliveryDAO.getDeliveries(onlyFor, filter));
            request.setAttribute("counts", deliveryDAO.countByStatus(onlyFor));
            request.setAttribute("filter", filter);
            request.getRequestDispatcher("/WEB-INF/views/delivery/manage-deliveries.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load the deliveries", e);
        }
    }

    // ================================================================ UPDATE

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        String action = TextUtil.clean(request.getParameter("action"));
        try {
            if (id == null) {
                throw new ValidationException("No delivery was selected.");
            }
            if (user.isAdmin()) {
                // Deliveries are the riders' job: the admin can look, but not change them.
                throw new ValidationException("Only the rider can update a delivery.");
            }
            String message;
            if ("pickup".equals(action)) {
                message = pickUp(user, id, request.getParameter("current"));
            } else if ("status".equals(action)) {
                message = updateStatus(user, id, request.getParameter("current"),
                        request.getParameter("next"), request.getParameter("note"));
            } else {
                throw new ValidationException("Unknown action.");
            }
            SessionUtil.flash(request, "success", message);
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not update the delivery", e);
        }

        String back = request.getParameter("returnTo");
        if (!TextUtil.isSafeLocalPath(back)) {
            back = id == null ? "/staff/deliveries" : "/staff/deliveries/view?id=" + id;
        }
        response.sendRedirect(request.getContextPath() + back);
    }

    /**
     * "Got the package": a rider takes a new parcel from the pharmacy.
     *
     * @param currentText the status the rider saw on the page (stops double clicks)
     */
    private String pickUp(User rider, int id, String currentText) throws SQLException, ValidationException {
        Delivery delivery = findForStaff(rider, id);
        if (delivery == null) {
            throw new ValidationException("That delivery was not found, or another rider has taken it.");
        }
        String ref = delivery.getOrderReference();
        if (DeliveryStatus.fromText(currentText) != delivery.getStatus()
                || delivery.getStatus() != DeliveryStatus.PENDING) {
            throw new ValidationException(ref + " is already \"" + delivery.getStatus().getLabel()
                    + "\". Please check it again.");
        }
        if (!deliveryDAO.pickUp(delivery, rider.getId(), rider.getFullName())) {
            throw new ValidationException(ref + " was just taken by another rider or cancelled. Please check it again.");
        }
        notificationDAO.addNotification(delivery.getCustomerId(), rider.getFullName() + " picked up your order " + ref
                + " from the pharmacy and is on the way to you. Please keep your phone nearby.",
                "/deliveries/track?orderId=" + delivery.getOrderId());
        return ref + " is now on the way. The customer has been notified.";
    }

    /**
     * The rider moves a delivery that is on the way to its next status.
     *
     * @param currentText the status the rider saw on the page - stops a double click moving it two steps
     * @param nextText    the status to move to (must be one of nextSteps())
     * @param noteText    optional note; for "could not deliver" the required reason
     */
    private String updateStatus(User rider, int id, String currentText, String nextText, String noteText)
            throws SQLException, ValidationException {
        Delivery delivery = findForStaff(rider, id);
        if (delivery == null) {
            throw new ValidationException("That delivery was not found.");
        }
        String ref = delivery.getOrderReference();

        // ---- validation
        if (DeliveryStatus.fromText(currentText) != delivery.getStatus()) {
            throw new ValidationException(ref + " was already changed to \"" + delivery.getStatus().getLabel()
                    + "\". Please check it again.");
        }
        DeliveryStatus next = DeliveryStatus.fromText(nextText);
        if (next == null || !delivery.getStatus().nextSteps().contains(next)) {
            throw new ValidationException(ref + " is \"" + delivery.getStatus().getLabel()
                    + "\" and cannot be moved to that step.");
        }
        if (delivery.getStatus() == DeliveryStatus.PENDING || !delivery.hasRider()) {
            // Leaving the pharmacy always goes through "Got the package".
            throw new ValidationException("Please press \"Got the package\" for " + ref + " first.");
        }
        String note = TextUtil.clean(noteText);
        if (next == DeliveryStatus.FAILED && note.length() < 5) {
            throw new ValidationException("Please write why the parcel could not be delivered (at least "
                    + "5 characters), e.g. \"Nobody at home\".");
        }
        if (note.length() > 300) {
            throw new ValidationException("The note can have at most 300 characters.");
        }

        if (!deliveryDAO.updateStatus(delivery, delivery.getStatus(), next, rider.getId(), note.isEmpty() ? null : note)) {
            throw new ValidationException(ref + " was just changed by someone else. Please check it again.");
        }
        notificationDAO.addNotification(delivery.getCustomerId(), customerMessage(delivery, next, note),
                "/deliveries/track?orderId=" + delivery.getOrderId());
        return ref + " is now \"" + next.getLabel() + "\". The customer has been notified.";
    }

    // =============================================================== helpers

    /**
     * A delivery the user may work on (with its updates), or null. A rider may
     * open their own deliveries and new ones nobody has taken yet; the admin all.
     */
    private Delivery findForStaff(User user, int id) throws SQLException {
        Delivery delivery = deliveryDAO.getDeliveryById(id);
        if (delivery == null) {
            return null;
        }
        boolean free = !delivery.hasRider() && delivery.getStatus() == DeliveryStatus.PENDING;
        if (!user.isAdmin() && !free && !Integer.valueOf(user.getId()).equals(delivery.getStaffId())) {
            return null;
        }
        return delivery;
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
