package com.medisys.service;

import com.medisys.dao.NotificationDAO;
import com.medisys.dao.impl.NotificationDAOImpl;
import com.medisys.model.Notification;

import java.sql.SQLException;
import java.util.List;

/**
 * Other modules call this to send a message to a user.
 *
 * Basic version added early, because module 05 must tell customers about the
 * pharmacist's decision.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 *
 * TODO (module 06): e-mail / SMS, delete old notifications.
 */
public class NotificationService {

    private static final int MESSAGE_MAX = 500;

    private final NotificationDAO notificationDAO = new NotificationDAOImpl();

    /**
     * Sends a message to a user.
     *
     * A failed notification must not undo the action that caused it (the
     * pharmacist's decision is already saved), so errors are only logged.
     *
     * @param link a page inside the app such as "/prescriptions", or null
     */
    public void notify(int userId, String message, String link) {
        String text = message.length() > MESSAGE_MAX ? message.substring(0, MESSAGE_MAX - 3) + "..." : message;
        try {
            notificationDAO.create(userId, text, link);
        } catch (SQLException e) {
            System.out.println("[MediSys] Could not save a notification for user " + userId + ": " + e.getMessage());
        }
    }

    public List<Notification> getLatest(int userId) throws SQLException {
        return notificationDAO.findByUser(userId, 50);
    }

    public int countUnread(int userId) throws SQLException {
        return notificationDAO.countUnread(userId);
    }

    public void markAllRead(int userId) throws SQLException {
        notificationDAO.markAllRead(userId);
    }
}
