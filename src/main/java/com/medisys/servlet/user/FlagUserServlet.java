package com.medisys.servlet.user;

import com.medisys.model.User;
import com.medisys.service.UserService;
import com.medisys.service.ValidationException;
import com.medisys.util.SessionUtil;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * A pharmacist or admin puts a red flag on a customer, or removes it.
 * A flag is only a warning for the staff - the customer is NOT banned.
 *
 *   POST /users/flag   id=3&action=flag&reason=Sends unrelated photos&returnTo=...
 *   POST /users/flag   id=3&action=unflag&returnTo=...
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/users/flag")
public class FlagUserServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User staff = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        String action = TextUtil.clean(request.getParameter("action"));
        try {
            if (id == null) {
                throw new ValidationException("No customer was selected.");
            }
            String message;
            if ("flag".equals(action)) {
                message = userService.flag(staff, id, request.getParameter("reason"));
            } else if ("unflag".equals(action)) {
                message = userService.clearFlag(staff, id);
            } else {
                throw new ValidationException("Unknown action.");
            }
            SessionUtil.flash(request, "success", message);
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not change the flag", e);
        }

        String back = request.getParameter("returnTo");
        if (!TextUtil.isSafeLocalPath(back)) {
            back = LoginServlet.homePath(staff);
        }
        response.sendRedirect(request.getContextPath() + back);
    }
}
