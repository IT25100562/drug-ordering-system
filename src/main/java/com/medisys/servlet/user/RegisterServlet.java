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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer self-registration.
 *
 *   GET  /register?returnTo=/prescriptions/upload
 *   POST /register   fullName, email, nic, dateOfBirth, phone, whatsapp, sameAsPhone,
 *                    password, confirmPassword, returnTo
 *
 * After registering, the customer is logged in and sent back to returnTo
 * (e.g. the prescription upload page they wanted to open).
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/user/register.jsp";

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        if (user != null) {
            response.sendRedirect(request.getContextPath() + LoginServlet.homePath(user));
            return;
        }
        showForm(request, response, new HashMap<>(), null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Map<String, String> form = new HashMap<>();
        for (String field : UserService.REGISTER_FIELDS) {
            form.put(field, request.getParameter(field));
        }
        try {
            User user = userService.register(form, request.getParameter("password"),
                    request.getParameter("confirmPassword"));
            SessionUtil.login(request, user);
            SessionUtil.flash(request, "success", "Welcome to MediSys, " + user.getFirstName()
                    + "! Your account is ready.");
            String returnTo = safeReturnTo(request);
            response.sendRedirect(request.getContextPath() + (returnTo.isEmpty() ? "/account/profile" : returnTo));
        } catch (ValidationException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showForm(request, response, form, e.getErrors());
        } catch (SQLException e) {
            throw new ServletException("Could not create the account", e);
        }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          Map<String, String> form, List<String> errors) throws ServletException, IOException {
        request.setAttribute("form", form);       // typed values are kept (never the password)
        request.setAttribute("errors", errors);
        request.setAttribute("returnTo", safeReturnTo(request));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private String safeReturnTo(HttpServletRequest request) {
        String returnTo = request.getParameter("returnTo");
        return TextUtil.isSafeLocalPath(returnTo) ? returnTo : "";
    }
}
