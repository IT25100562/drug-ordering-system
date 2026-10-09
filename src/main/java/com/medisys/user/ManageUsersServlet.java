package com.medisys.user;

import com.medisys.common.PasswordUtil;
import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.ValidationException;
import com.medisys.common.Validator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The admin's users page, and the red flag.
 *
 *   GET  /admin/users                        list users (filter + search)          READ
 *   POST /admin/users  action=create         add a staff account                   CREATE
 *   POST /admin/users  action=reset          give a staff member a new password    UPDATE
 *   POST /admin/users  action=activate       let a staff member log in again       UPDATE
 *   POST /admin/users  action=deactivate     stop a staff member from logging in   UPDATE
 *   POST /users/flag   action=flag / unflag  red flag on a customer (pharmacist or admin)
 *
 * Customers are never switched off. A customer who misuses the system gets a
 * red flag instead (a reason is required): only staff see it, and the customer
 * can still use everything.
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet({"/admin/users", "/users/flag"})
public class ManageUsersServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/user/manage-users.jsp";

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showPage(request, response, new HashMap<>(), null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User staff = SessionUtil.currentUser(request);
        String action = TextUtil.clean(request.getParameter("action"));
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        boolean flagPage = request.getServletPath().equals("/users/flag");

        try {
            String message;
            if (flagPage) {
                message = changeFlag(staff, id, action, request.getParameter("reason"));
            } else if ("create".equals(action)) {
                Map<String, String> form = new HashMap<>();
                for (String field : new String[]{"fullName", "email", "phone", "role"}) {
                    form.put(field, request.getParameter(field));
                }
                try {
                    message = createStaff(form, request.getParameter("password"));
                } catch (ValidationException e) {
                    // Show the form again with every problem and the typed values.
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    showPage(request, response, form, e.getErrors());
                    return;
                }
            } else if ("reset".equals(action)) {
                message = resetStaffPassword(id, request.getParameter("password"));
            } else if ("activate".equals(action) || "deactivate".equals(action)) {
                message = setActive(staff, id, "activate".equals(action));
            } else {
                throw new ValidationException("Unknown action.");
            }
            SessionUtil.flash(request, "success", message);
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not update the users", e);
        }

        String back = request.getParameter("returnTo");
        if (!TextUtil.isSafeLocalPath(back)) {
            back = flagPage ? LoginServlet.homePath(staff) : "/admin/users";
        }
        response.sendRedirect(request.getContextPath() + back);
    }

    // ================================================================ CREATE

    /** Adds a pharmacist / delivery staff / admin account. */
    private String createStaff(Map<String, String> form, String password) throws SQLException, ValidationException {
        // ---- validation
        List<String> errors = new ArrayList<>();
        String name = Validator.name(form.get("fullName"), errors);
        String email = TextUtil.clean(form.get("email")).toLowerCase();
        if (!Validator.isEmail(email)) {
            errors.add("Please enter a valid email address.");
        } else if (userDAO.emailExists(email, 0)) {
            errors.add("An account with this email already exists.");
        }
        String phone = Validator.phone(form.get("phone"), "phone number", false, false, errors);
        Role role = Role.fromText(form.get("role"));
        if (role == Role.CUSTOMER || !String.valueOf(form.get("role")).equalsIgnoreCase(role.name())) {
            errors.add("Please choose the staff role.");
        }
        Validator.password(password, password, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress("MediSys Pharmacy");
        user.setRole(role);
        userDAO.addUser(user, PasswordUtil.hash(password));
        return "The account for " + name + " (" + role.getLabel() + ") was created. Give them the password in person.";
    }

    // ================================================================ UPDATE

    /** The admin gives a staff member a new password (staff have no NIC to prove who they are). */
    private String resetStaffPassword(Integer id, String password) throws SQLException, ValidationException {
        User user = id == null ? null : userDAO.getUserById(id);
        if (user == null || user.isCustomer()) {
            throw new ValidationException("Only staff passwords can be reset here. Customers use \"Forgot password\".");
        }
        List<String> errors = new ArrayList<>();
        Validator.password(password, password, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        userDAO.updatePassword(id, PasswordUtil.hash(password));
        return "The password of " + user.getFullName() + " was changed. Give them the new password in person.";
    }

    /** Switches a staff account on or off. Customers are never switched off (they get a red flag instead). */
    private String setActive(User admin, Integer id, boolean active) throws SQLException, ValidationException {
        User user = id == null ? null : userDAO.getUserById(id);
        if (user == null) {
            throw new ValidationException("That account was not found.");
        }
        if (user.isCustomer()) {
            throw new ValidationException("Customer accounts are never switched off. Use a red flag instead.");
        }
        if (user.getId() == admin.getId()) {
            throw new ValidationException("You can't switch off your own account.");
        }
        userDAO.setActive(id, active);
        return user.getFullName() + (active ? " can log in again." : " can no longer log in.");
    }

    /** A pharmacist or admin puts a red flag on a customer, or removes it. */
    private String changeFlag(User staff, Integer id, String action, String reasonText)
            throws SQLException, ValidationException {
        User customer = id == null ? null : userDAO.getUserById(id);
        if (customer == null || !customer.isCustomer()) {
            throw new ValidationException("That customer was not found.");
        }
        if ("unflag".equals(action)) {
            if (!userDAO.removeFlag(id)) {
                throw new ValidationException(customer.getFullName() + " is not flagged.");
            }
            return "The red flag was removed from " + customer.getFullName() + ".";
        }
        if (!"flag".equals(action)) {
            throw new ValidationException("Unknown action.");
        }
        String reason = TextUtil.clean(reasonText);
        if (reason.length() < 5 || reason.length() > 300) {
            throw new ValidationException("Please write why this customer is flagged (5 to 300 characters), "
                    + "so other staff know.");
        }
        if (customer.isFlagged()) {
            throw new ValidationException(customer.getFullName() + " is already flagged.");
        }
        userDAO.flagCustomer(id, reason, staff.getId());
        return customer.getFullName() + " is now flagged in red. They can still use MediSys.";
    }

    // ================================================================== READ

    private void showPage(HttpServletRequest request, HttpServletResponse response,
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
            request.setAttribute("users", userDAO.getUsers(filter, keyword));
            request.setAttribute("counts", userDAO.countUsers());
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
