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
 * Shows the login form and logs the user in.
 *
 *   GET  /login?returnTo=/cart
 *   POST /login   email, password, returnTo
 *
 * Basic version added early so the cart (module 01) can work.
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/user/login.jsp";

    private final UserService userService = new UserService();

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
        String email = request.getParameter("email");
        String returnTo = safeReturnTo(request);

        try {
            User user = userService.login(email, request.getParameter("password"));
            SessionUtil.login(request, user);
            SessionUtil.flash(request, "success", "Welcome back, " + user.getFirstName() + "!");

            // Customers go back to where they were; staff go to their own page.
            String target = user.isCustomer() && !returnTo.isEmpty() ? returnTo : homePath(user);
            response.sendRedirect(request.getContextPath() + target);
        } catch (ValidationException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("email", TextUtil.clean(email));
            request.setAttribute("returnTo", returnTo);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not log in", e);
        }
    }

    /** The first page each role sees after login. */
    static String homePath(User user) {
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

    private String safeReturnTo(HttpServletRequest request) {
        String returnTo = request.getParameter("returnTo");
        return TextUtil.isSafeLocalPath(returnTo) ? returnTo : "";
    }
}
