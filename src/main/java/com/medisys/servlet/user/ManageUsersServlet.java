package com.medisys.servlet.user;

import com.medisys.dao.UserDAO;
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
 * Admin page: who uses MediSys (photo, name, role, red flag), add staff
 * accounts, switch staff accounts off / on.
 * Personal details (NIC, date of birth, phone) are NOT listed here - the
 * pharmacist sees them next to the prescription.
 *
 *   GET  /admin/users?show=CUSTOMERS&q=nimal
 *   POST /admin/users   action=create      fullName, email, phone, role, password
 *   POST /admin/users   action=deactivate  id      (staff only)
 *   POST /admin/users   action=activate    id
 *   POST /admin/users   action=reset       id, password   (staff only)
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/admin/users")
public class ManageUsersServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/user/manage-users.jsp";

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        show(request, response, new HashMap<>(), null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User admin = SessionUtil.currentUser(request);
        String action = TextUtil.clean(request.getParameter("action"));
        Map<String, String> form = new HashMap<>();
        try {
            if ("create".equals(action)) {
                for (String field : new String[]{"fullName", "email", "phone", "role"}) {
                    form.put(field, request.getParameter(field));
                }
                try {
                    User created = userService.createStaff(admin, form, request.getParameter("password"));
                    SessionUtil.flash(request, "success", "The account for " + created.getFullName() + " ("
                            + created.getRole().getLabel() + ") was created. Give them the password in person.");
                } catch (ValidationException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    show(request, response, form, e.getErrors());
                    return;
                }
            } else {
                Integer id = TextUtil.parseInt(request.getParameter("id"));
                try {
                    if (id == null) {
                        throw new ValidationException("No account was selected.");
                    } else if ("reset".equals(action)) {
                        SessionUtil.flash(request, "success",
                                userService.resetStaffPassword(admin, id, request.getParameter("password")));
                    } else if ("activate".equals(action) || "deactivate".equals(action)) {
                        SessionUtil.flash(request, "success", userService.setActive(admin, id, "activate".equals(action)));
                    } else {
                        throw new ValidationException("Unknown action.");
                    }
                } catch (ValidationException e) {
                    SessionUtil.flash(request, "error", e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new ServletException("Could not update the users", e);
        }

        String back = request.getParameter("returnTo");
        response.sendRedirect(request.getContextPath() + (TextUtil.isSafeLocalPath(back) ? back : "/admin/users"));
    }

    private void show(HttpServletRequest request, HttpServletResponse response,
                      Map<String, String> form, List<String> errors) throws ServletException, IOException {
        String filter = TextUtil.clean(request.getParameter("show")).toUpperCase();
        if (!List.of(UserDAO.FILTER_CUSTOMERS, UserDAO.FILTER_STAFF, UserDAO.FILTER_FLAGGED).contains(filter)) {
            filter = UserDAO.FILTER_ALL;
        }
        String keyword = TextUtil.clean(request.getParameter("q"));
        if (keyword.length() > 100) {
            keyword = keyword.substring(0, 100);
        }
        try {
            request.setAttribute("users", userService.getUsers(filter, keyword));
            request.setAttribute("counts", userService.getCounts());
        } catch (SQLException e) {
            throw new ServletException("Could not load the users", e);
        }
        request.setAttribute("filter", filter);
        request.setAttribute("keyword", keyword);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
