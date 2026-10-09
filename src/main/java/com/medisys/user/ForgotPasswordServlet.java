package com.medisys.user;

import com.medisys.common.PasswordUtil;
import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.Validator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Forgot password (customers).
 *
 *   GET  /forgot-password    the form
 *   POST /forgot-password    check email + NIC + date of birth, then save the new password
 *
 * There is no e-mail server, so no reset link can be sent. The customer
 * proves who they are with the details they registered with. Staff ask the
 * admin, who resets their password on the users page.
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/user/forgot-password.jsp";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (SessionUtil.currentUser(request) != null) {
            // Logged in users change their password on the profile page.
            response.sendRedirect(request.getContextPath() + "/account/profile#password");
            return;
        }
        show(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = TextUtil.clean(request.getParameter("email")).toLowerCase();
        String nic = TextUtil.clean(request.getParameter("nic")).toUpperCase().replace(" ", "");
        LocalDate born = TextUtil.parseDate(request.getParameter("dateOfBirth"));
        String password = request.getParameter("password");

        try {
            // ---- validation
            List<String> errors = new ArrayList<>();
            User user = null;
            if (email.isEmpty() || nic.isEmpty() || born == null) {
                errors.add("Please enter your email, NIC number and date of birth.");
            } else {
                user = userDAO.getUserByEmail(email);
                // The same message for every mismatch, so the page does not reveal which part was wrong.
                boolean matches = user != null && user.isCustomer()
                        && nic.equals(user.getNic()) && born.equals(user.getDateOfBirth());
                if (!matches) {
                    errors.add("These details do not match any customer account. "
                            + "Staff members: please ask the administrator to reset your password.");
                } else {
                    Validator.password(password, request.getParameter("confirmPassword"), errors);
                }
            }
            if (!errors.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                show(request, response, errors);
                return;
            }

            // UPDATE
            userDAO.updatePassword(user.getId(), PasswordUtil.hash(password));
            SessionUtil.flash(request, "success", "Your password was changed. Please log in with the new password.");
            response.sendRedirect(request.getContextPath() + "/login");
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
