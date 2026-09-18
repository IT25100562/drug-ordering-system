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
import java.util.List;

/**
 * Forgot password: a customer sets a new password after proving who they are
 * with their email, NIC and date of birth.
 *
 *   GET  /forgot-password
 *   POST /forgot-password   email, nic, dateOfBirth, password, confirmPassword
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/user/forgot-password.jsp";

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        if (user != null) {
            // Logged in users change their password on the profile page.
            response.sendRedirect(request.getContextPath() + "/account/profile#password");
            return;
        }
        show(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            userService.resetForgottenPassword(request.getParameter("email"), request.getParameter("nic"),
                    request.getParameter("dateOfBirth"), request.getParameter("password"),
                    request.getParameter("confirmPassword"));
            SessionUtil.flash(request, "success", "Your password was changed. Please log in with the new password.");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (ValidationException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            show(request, response, e.getErrors());
        } catch (SQLException e) {
            throw new ServletException("Could not reset the password", e);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, List<String> errors)
            throws ServletException, IOException {
        request.setAttribute("errors", errors);
        // Typed values are kept, except the passwords.
        request.setAttribute("email", TextUtil.clean(request.getParameter("email")));
        request.setAttribute("nic", TextUtil.clean(request.getParameter("nic")));
        request.setAttribute("dateOfBirth", TextUtil.clean(request.getParameter("dateOfBirth")));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
