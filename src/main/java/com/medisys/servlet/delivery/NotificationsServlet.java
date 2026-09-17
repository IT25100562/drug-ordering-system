package com.medisys.servlet.delivery;

import com.medisys.model.User;
import com.medisys.service.NotificationService;
import com.medisys.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Shows the logged in user's notifications, then marks them as read.
 *
 *   GET /notifications
 *
 * Basic version added early for module 05.
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
@WebServlet("/notifications")
public class NotificationsServlet extends HttpServlet {

    private final NotificationService notificationService = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            // Load first (so the page can still show which ones are new), then mark them read.
            request.setAttribute("notifications", notificationService.getLatest(user.getId()));
            notificationService.markAllRead(user.getId());
        } catch (SQLException e) {
            throw new ServletException("Could not load the notifications", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/delivery/notifications.jsp").forward(request, response);
    }
}
