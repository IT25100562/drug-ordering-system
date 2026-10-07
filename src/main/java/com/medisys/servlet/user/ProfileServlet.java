package com.medisys.servlet.user;

import com.medisys.model.User;
import com.medisys.service.OrderService;
import com.medisys.service.PrescriptionService;
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
 * The logged in user's own profile: photo, details, password and (for
 * customers) their latest prescriptions and orders.
 *
 *   GET  /account/profile
 *   POST /account/profile   action=details   fullName, phone, whatsapp, address
 *   POST /account/profile   action=password  currentPassword, newPassword, confirmPassword
 *
 * The photo is changed by ProfilePhotoServlet (/account/photo).
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/account/profile")
public class ProfileServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/user/profile.jsp";
    private static final int RECENT = 5;

    private final UserService userService = new UserService();
    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User user = userService.getUser(SessionUtil.currentUser(request).getId());
            SessionUtil.refreshUser(request, user);
            show(request, response, user, null, null, null);
        } catch (SQLException e) {
            throw new ServletException("Could not load the profile", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        String action = TextUtil.clean(request.getParameter("action"));
        Map<String, String> form = new HashMap<>();
        try {
            try {
                if ("details".equals(action)) {
                    for (String field : new String[]{"fullName", "phone", "whatsapp", "address"}) {
                        form.put(field, request.getParameter(field));
                    }
                    User changed = userService.updateContact(user, form);
                    SessionUtil.refreshUser(request, changed);
                    SessionUtil.flash(request, "success", "Your details were saved.");
                } else if ("password".equals(action)) {
                    userService.changePassword(user, request.getParameter("currentPassword"),
                            request.getParameter("newPassword"), request.getParameter("confirmPassword"));
                    SessionUtil.flash(request, "success", "Your password was changed.");
                } else {
                    throw new ValidationException("Unknown action.");
                }
                response.sendRedirect(request.getContextPath() + "/account/profile");
            } catch (ValidationException e) {
                // Show the page again with the errors next to the form that caused them.
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                show(request, response, userService.getUser(user.getId()), action, e.getErrors(), form);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not save the profile", e);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, User user,
                      String errorForm, List<String> errors, Map<String, String> form)
            throws SQLException, ServletException, IOException {
        request.setAttribute("profile", user);
        request.setAttribute("errorForm", errorForm);
        request.setAttribute("errors", errors);
        request.setAttribute("form", form);
        if (user.isCustomer()) {
            request.setAttribute("prescriptions", first(prescriptionService.getMyPrescriptions(user.getId())));
            request.setAttribute("orders", first(orderService.getMyOrders(user.getId())));
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /** The newest few (the lists are already newest first). */
    private static <T> List<T> first(List<T> list) {
        return list.size() > RECENT ? list.subList(0, RECENT) : list;
    }
}
