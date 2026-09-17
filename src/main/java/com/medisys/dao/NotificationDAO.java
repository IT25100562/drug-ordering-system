package com.medisys.dao;

import com.medisys.model.Notification;

import java.sql.SQLException;
import java.util.List;

/**
 * Database operations for notifications.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
public interface NotificationDAO {

    void create(int userId, String message, String link) throws SQLException;

    /** The user's newest notifications (at most "limit"). */
    List<Notification> findByUser(int userId, int limit) throws SQLException;

    int countUnread(int userId) throws SQLException;

    void markAllRead(int userId) throws SQLException;
}
