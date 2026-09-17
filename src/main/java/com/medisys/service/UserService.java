package com.medisys.service;

import com.medisys.dao.UserDAO;
import com.medisys.dao.impl.UserDAOImpl;
import com.medisys.model.User;
import com.medisys.util.PasswordUtil;
import com.medisys.util.TextUtil;

import java.sql.SQLException;

/**
 * Business rules for users.
 *
 * Only login is here so far (added early so the cart can work).
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 *
 * TODO: register (unique email, password rules), updateProfile,
 *       changePassword, changeRole, deactivate.
 */
public class UserService {

    private static final String DUMMY_HASH = PasswordUtil.hash("not-a-real-password");

    private final UserDAO userDAO = new UserDAOImpl();

    /**
     * Checks the email and password.
     * The same message is used for "no such email" and "wrong password", so the
     * page does not tell an attacker which emails are registered.
     */
    public User login(String emailText, String password) throws SQLException, ValidationException {
        String email = TextUtil.clean(emailText).toLowerCase();
        if (email.isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Please enter your email and password.");
        }

        User user = userDAO.findByEmail(email);
        // For an unknown email, still check against a dummy hash, so both cases
        // take the same time.
        String storedHash = user == null ? DUMMY_HASH : userDAO.findPasswordHash(user.getId());
        boolean passwordOk = PasswordUtil.matches(password, storedHash);
        if (user == null || !passwordOk) {
            throw new ValidationException("The email or password is incorrect.");
        }
        if (!user.isActive()) {
            throw new ValidationException("This account has been deactivated. Please contact the pharmacy.");
        }
        return user;
    }
}
