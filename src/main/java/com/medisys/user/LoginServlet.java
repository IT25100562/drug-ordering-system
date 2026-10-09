package com.medisys.user;

import com.medisys.common.PasswordUtil;
import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Log in and log out.
 *
 *   GET  /login     the login form
 *   POST /login     check the email + password, then go to the user's first page
 *   POST /logout    end the session
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet({"/login", "/logout"})
public class LoginServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/user/login.jsp";

    // Checked when the email is unknown, so both cases take the same time.
    private static final String DUMMY_HASH = PasswordUtil.hash("not-a-real-password");

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        if (user != null) {
            response.sendRedirect(request.getContextPath() + homePath(user));
            return;
        }
        request.setAttribute("returnTo", safeReturnTo(request));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getServletPath().equals("/logout")) {
            SessionUtil.logout(request);
            SessionUtil.flash(request, "success", "You have been logged out.");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String email = TextUtil.clean(request.getParameter("email")).toLowerCase();
        String password = request.getParameter("password");
        String returnTo = safeReturnTo(request);

        try {
            // ---- validation
            String error = null;
            User user = null;
            if (email.isEmpty() || password == null || password.isEmpty()) {
                error = "Please enter your email and password.";
            } else {
                user = userDAO.getUserByEmail(email);
                String storedHash = user == null ? DUMMY_HASH : userDAO.getPasswordHash(user.getId());
                boolean passwordOk = PasswordUtil.matches(password, storedHash);
                // The same message for "no such email" and "wrong password", so the
                // page does not tell an attacker which emails are registered.
                if (user == null || !passwordOk) {
                    error = "The email or password is incorrect.";
                } else if (!user.isActive()) {
                    error = "This account has been deactivated. Please contact the pharmacy.";
                }
            }
            if (error != null) {
                request.setAttribute("error", error);
                request.setAttribute("email", email);
                request.setAttribute("returnTo", returnTo);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }

            // ---- logged in
            SessionUtil.login(request, user);
            SessionUtil.flash(request, "success", "Welcome back, " + user.getFirstName() + "!");
            // Customers go back to where they were; staff go to their own page.
            String target = user.isCustomer() && !returnTo.isEmpty() ? returnTo : homePath(user);
            response.sendRedirect(request.getContextPath() + target);
        } catch (SQLException e) {
            throw new ServletException("Could not log in", e);
        }
    }

    /** The first page each role sees after login. */
    public static String homePath(User user) {
        switch (user.getRole()) {
            case ADMIN:
                return "/admin/medicines";
            case PHARMACIST:
                return "/pharmacist/dashboard";
            case DELIVERY_STAFF:
                return "/staff/deliveries";
            default:
                return "/medicines";
        }
    }

    /** The "returnTo" parameter if it is a safe page inside this app, otherwise "". */
    static String safeReturnTo(HttpServletRequest request) {
        String returnTo = request.getParameter("returnTo");
        return TextUtil.isSafeLocalPath(returnTo) ? returnTo : "";
    }
}
