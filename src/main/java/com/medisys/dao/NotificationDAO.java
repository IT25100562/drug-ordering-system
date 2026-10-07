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

    /** Deletes one of the user's notifications. False if it is not theirs (or already gone). */
    boolean delete(int id, int userId) throws SQLException;

    /** Deletes all of the user's read notifications. Returns how many were deleted. */
    int deleteRead(int userId) throws SQLException;
}
