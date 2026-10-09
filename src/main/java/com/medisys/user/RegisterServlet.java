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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer registration (CREATE a user).
 *
 *   GET  /register    the form
 *   POST /register    validate, save the account and log the customer in
 *
 * Rules:
 *  - name, email, password, NIC, date of birth, phone, WhatsApp - nothing more
 *  - email and NIC can only be used by one account
 *  - NIC: old (123456789V) or new (200012345678); its birth year must match
 *    the date of birth
 *  - at least 18 years old; password at least 8 characters with a letter and a digit
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/user/register.jsp";
    private static final String[] FIELDS = {"fullName", "email", "nic", "dateOfBirth", "phone", "whatsapp", "sameAsPhone"};

    private final UserDAO userDAO = new UserDAO();

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
        for (String field : FIELDS) {
            form.put(field, request.getParameter(field));
        }
        try {
            User user = new User();
            List<String> errors = validate(form, request.getParameter("password"),
                    request.getParameter("confirmPassword"), user);
            if (!errors.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                showForm(request, response, form, errors);
                return;
            }

            // CREATE
            int id = userDAO.addUser(user, PasswordUtil.hash(request.getParameter("password")));
            user = userDAO.getUserById(id);

            SessionUtil.login(request, user);
            SessionUtil.flash(request, "success", "Welcome to MediSys, " + user.getFirstName()
                    + "! Your account is ready.");
            String returnTo = LoginServlet.safeReturnTo(request);
            response.sendRedirect(request.getContextPath() + (returnTo.isEmpty() ? "/account/profile" : returnTo));
        } catch (SQLException e) {
            throw new ServletException("Could not create the account", e);
        }
    }

    /** VALIDATION: checks the form and fills in "user". Returns every problem found. */
    private List<String> validate(Map<String, String> form, String password, String confirm, User user)
            throws SQLException {
        List<String> errors = new ArrayList<>();

        String name = Validator.name(form.get("fullName"), errors);

        String email = TextUtil.clean(form.get("email")).toLowerCase();
        if (!Validator.isEmail(email)) {
            errors.add("Please enter a valid email address.");
        } else if (userDAO.emailExists(email, 0)) {
            errors.add("An account with this email already exists. Please log in instead.");
        }

        LocalDate born = TextUtil.parseDate(form.get("dateOfBirth"));
        if (born == null) {
            errors.add("Please enter your date of birth.");
        } else if (born.isAfter(LocalDate.now().minusYears(Validator.MIN_AGE))) {
            errors.add("You must be at least " + Validator.MIN_AGE + " years old to register.");
        } else if (born.isBefore(LocalDate.now().minusYears(120))) {
            errors.add("Please check your date of birth.");
        }

        String nic = TextUtil.clean(form.get("nic")).toUpperCase().replace(" ", "");
        Integer nicYear = Validator.nicBirthYear(nic);
        if (nicYear == null) {
            errors.add("Please enter a valid NIC number: 9 digits and V or X (e.g. 951234567V), or 12 digits.");
        } else if (born != null && born.getYear() != nicYear) {
            errors.add("The NIC number does not match your date of birth (the NIC says " + nicYear + ").");
        } else if (userDAO.nicExists(nic, 0)) {
            errors.add("An account with this NIC number already exists. Please log in instead.");
        }

        String phone = Validator.phone(form.get("phone"), "phone number", true, false, errors);
        String whatsapp = "on".equals(form.get("sameAsPhone")) && phone != null && phone.startsWith("07")
                ? phone
                : Validator.phone(form.get("whatsapp"), "WhatsApp number", false, true, errors);

        Validator.password(password, confirm, errors);

        user.setFullName(name);
        user.setEmail(email);
        user.setNic(nic);
        user.setDateOfBirth(born);
        user.setPhone(phone);
        user.setWhatsapp(whatsapp);
        user.setRole(Role.CUSTOMER);
        return errors;
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response,
                          Map<String, String> form, List<String> errors) throws ServletException, IOException {
        request.setAttribute("form", form);       // typed values are kept (never the password)
        request.setAttribute("errors", errors);
        request.setAttribute("returnTo", LoginServlet.safeReturnTo(request));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
