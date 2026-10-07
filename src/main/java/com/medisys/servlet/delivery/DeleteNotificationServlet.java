package com.medisys.servlet.delivery;

import com.medisys.model.User;
import com.medisys.service.NotificationService;
import com.medisys.service.ValidationException;
import com.medisys.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Deletes the logged in user's notifications.
 *
 *   POST /notifications/delete   id=5          one notification
 *   POST /notifications/delete   all=read      every notification already read
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
@WebServlet("/notifications/delete")
public class DeleteNotificationServlet extends HttpServlet {

    private final NotificationService notificationService = new NotificationService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            String message = "read".equals(request.getParameter("all"))
                    ? notificationService.deleteRead(user.getId())
                    : notificationService.delete(user.getId(), request.getParameter("id"));
            SessionUtil.flash(request, "success", message);
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not delete the notification", e);
        }
        response.sendRedirect(request.getContextPath() + "/notifications");
    }
}
