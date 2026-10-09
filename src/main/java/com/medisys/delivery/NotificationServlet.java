package com.medisys.delivery;

import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.user.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * The user's notifications (every role has them).
 *
 *   GET  /notifications                       the newest 50, then marks them read   READ + UPDATE
 *   POST /notifications/delete  id=5          delete one                            DELETE
 *   POST /notifications/delete  all=read      delete every read one                 DELETE
 *
 * Module : 06 - Delivery Tracking and Notification
 * Owner  : Deshabhi R. G. S.
 */
@WebServlet({"/notifications", "/notifications/delete"})
public class NotificationServlet extends HttpServlet {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            // Load first (so the page can still show which ones are new), then mark them read.
            request.setAttribute("notifications", notificationDAO.getNotifications(user.getId(), 50));
            notificationDAO.markAllRead(user.getId());
        } catch (SQLException e) {
            throw new ServletException("Could not load the notifications", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/delivery/notifications.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            if ("read".equals(request.getParameter("all"))) {
                int count = notificationDAO.deleteReadNotifications(user.getId());
                SessionUtil.flash(request, "success", count == 0 ? "There were no old notifications to clear."
                        : count + " old notification" + (count == 1 ? " was" : "s were") + " cleared.");
            } else {
                Integer id = TextUtil.parseInt(request.getParameter("id"));
                if (id != null && notificationDAO.deleteNotification(id, user.getId())) {
                    SessionUtil.flash(request, "success", "Notification removed.");
                } else {
                    SessionUtil.flash(request, "error", "That notification was already removed.");
                }
            }
        } catch (SQLException e) {
            throw new ServletException("Could not delete the notification", e);
        }
        response.sendRedirect(request.getContextPath() + "/notifications");
    }
}
