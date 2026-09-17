package com.medisys.service;

import com.medisys.dao.UserDAO;
import com.medisys.dao.impl.UserDAOImpl;
import com.medisys.model.Role;
import com.medisys.model.User;
import com.medisys.storage.StorageFactory;
import com.medisys.util.PasswordUtil;
import com.medisys.util.TextUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * All business rules of User and Role Management.
 *
 * Registration (customers)
 *  - name, email, password, NIC, date of birth, phone, WhatsApp - nothing more
 *  - email and NIC can only be used by one account
 *  - NIC: old (123456789V) or new (200012345678); its birth year must match
 *    the date of birth
 *  - at least 18 years old; password at least 8 characters with a letter and a digit
 *
 * Forgot password
 *  - a customer resets it with their email + NIC + date of birth
 *  - a staff member's password is reset by the admin
 *
 * Profile
 *  - a user can change their name, phone, WhatsApp, address, password and photo
 *  - NIC, date of birth and email identify the person, so they can't be changed online
 *  - profile photo: JPG or PNG, at most 2 MB; only the user, pharmacists and
 *    admins can see it
 *
 * Staff
 *  - the admin adds pharmacist / delivery / admin accounts and can switch staff
 *    accounts off (not their own). Customers are never switched off.
 *  - a pharmacist or admin can put a red flag on a customer who misuses the
 *    system (a reason is required). The customer can still use everything and
 *    never sees the flag.
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 */
public class UserService {

    public static final int MIN_AGE = 18;
    public static final int PASSWORD_MIN = 8;
    public static final int FLAG_REASON_MIN = 5;
    public static final int FLAG_REASON_MAX = 300;
    public static final int MAX_PHOTO_BYTES = 2 * 1024 * 1024;

    /** The fields of the registration form (register.jsp). */
    public static final String[] REGISTER_FIELDS = {
            "fullName", "email", "nic", "dateOfBirth", "phone", "whatsapp", "sameAsPhone"
    };

    private static final String PHOTO_FOLDER = "profiles";
    private static final String DUMMY_HASH = PasswordUtil.hash("not-a-real-password");

    private final UserDAO userDAO = new UserDAOImpl();

    // ============================================================== login

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

    // ======================================================= registration

    /**
     * Creates a customer account.
     *
     * @param form the REGISTER_FIELDS
     * @return the new user (ready to be logged in)
     */
    public User register(Map<String, String> form, String password, String confirm)
            throws SQLException, ValidationException {
        List<String> errors = new ArrayList<>();

        String name = checkName(form.get("fullName"), errors);
        String email = TextUtil.clean(form.get("email")).toLowerCase();
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}") || email.length() > 150) {
            errors.add("Please enter a valid email address.");
        } else if (userDAO.emailTaken(email, 0)) {
            errors.add("An account with this email already exists. Please log in instead.");
        }

        LocalDate born = TextUtil.parseDate(form.get("dateOfBirth"));
        if (born == null) {
            errors.add("Please enter your date of birth.");
        } else if (born.isAfter(LocalDate.now().minusYears(MIN_AGE))) {
            errors.add("You must be at least " + MIN_AGE + " years old to register.");
        } else if (born.isBefore(LocalDate.now().minusYears(120))) {
            errors.add("Please check your date of birth.");
        }

        String nic = TextUtil.clean(form.get("nic")).toUpperCase().replace(" ", "");
        Integer nicYear = nicBirthYear(nic);
        if (nicYear == null) {
            errors.add("Please enter a valid NIC number: 9 digits and V or X (e.g. 951234567V), or 12 digits.");
        } else if (born != null && born.getYear() != nicYear) {
            errors.add("The NIC number does not match your date of birth (the NIC says " + nicYear + ").");
        } else if (userDAO.nicTaken(nic, 0)) {
            errors.add("An account with this NIC number already exists. Please log in instead.");
        }

        String phone = checkPhone(form.get("phone"), "phone number", true, false, errors);
        String whatsapp = "on".equals(form.get("sameAsPhone")) && phone != null && phone.startsWith("07")
                ? phone
                : checkPhone(form.get("whatsapp"), "WhatsApp number", false, true, errors);

        checkNewPassword(password, confirm, errors);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setNic(nic);
        user.setDateOfBirth(born);
        user.setPhone(phone);
        user.setWhatsapp(whatsapp);
        user.setRole(Role.CUSTOMER);
        int id = userDAO.create(user, PasswordUtil.hash(password));
        return userDAO.findById(id);
    }

    /**
     * The birth year hidden in a Sri Lankan NIC, or null if the NIC is not valid.
     *   old: YYDDDnnnnV   -> 19YY      new: YYYYDDDnnnnn -> YYYY
     * DDD is the day of the year (500 is added for women), so it must be 1-366 or 501-866.
     */
    static Integer nicBirthYear(String nic) {
        int year;
        int day;
        if (nic.matches("\\d{9}[VX]")) {
            year = 1900 + Integer.parseInt(nic.substring(0, 2));
            day = Integer.parseInt(nic.substring(2, 5));
        } else if (nic.matches("\\d{12}")) {
            year = Integer.parseInt(nic.substring(0, 4));
            day = Integer.parseInt(nic.substring(4, 7));
        } else {
            return null;
        }
        if (day > 500) {
            day -= 500;
        }
        return day >= 1 && day <= 366 ? year : null;
    }

    // ==================================================== forgot password

    /**
     * A customer who forgot their password proves who they are with the
     * email, NIC and date of birth they registered with, then picks a new one.
     * (There is no e-mail server, so no reset link can be sent.)
     * The same message is used for every mismatch, so the page does not
     * reveal which part was wrong.
     */
    public User resetForgottenPassword(String emailText, String nicText, String dateText,
                                       String password, String confirm) throws SQLException, ValidationException {
        String email = TextUtil.clean(emailText).toLowerCase();
        String nic = TextUtil.clean(nicText).toUpperCase().replace(" ", "");
        LocalDate born = TextUtil.parseDate(dateText);
        if (email.isEmpty() || nic.isEmpty() || born == null) {
            throw new ValidationException("Please enter your email, NIC number and date of birth.");
        }

        User user = userDAO.findByEmail(email);
        boolean matches = user != null && user.isCustomer()
                && nic.equals(user.getNic()) && born.equals(user.getDateOfBirth());
        if (!matches) {
            throw new ValidationException("These details do not match any customer account. "
                    + "Staff members: please ask the administrator to reset your password.");
        }

        List<String> errors = new ArrayList<>();
        checkNewPassword(password, confirm, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        userDAO.updatePassword(user.getId(), PasswordUtil.hash(password));
        return user;
    }

    /** The admin gives a staff member a new password (staff have no NIC to prove who they are). */
    public String resetStaffPassword(User admin, int id, String password) throws SQLException, ValidationException {
        checkAdmin(admin);
        User user = userDAO.findById(id);
        if (user == null || user.isCustomer()) {
            throw new ValidationException("Only staff passwords can be reset here. Customers use \"Forgot password\".");
        }
        List<String> errors = new ArrayList<>();
        checkNewPassword(password, password, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        userDAO.updatePassword(id, PasswordUtil.hash(password));
        return "The password of " + user.getFullName() + " was changed. Give them the new password in person.";
    }

    // ============================================================ profile

    public User getUser(int id) throws SQLException {
        return userDAO.findById(id);
    }

    /** Saves the details a user may change and returns the fresh user. */
    public User updateContact(User current, Map<String, String> form) throws SQLException, ValidationException {
        List<String> errors = new ArrayList<>();
        String name = checkName(form.get("fullName"), errors);
        boolean customer = current.isCustomer();
        String phone = checkPhone(form.get("phone"), "phone number", customer, false, errors);
        String whatsapp = checkPhone(form.get("whatsapp"), "WhatsApp number", false, true, errors);
        String address = TextUtil.clean(form.get("address"));
        if (address.length() > 255) {
            errors.add("The address can have at most 255 characters.");
        } else if (!address.isEmpty() && address.length() < 5) {
            errors.add("Please enter the full address (or leave it empty).");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        User changed = userDAO.findById(current.getId());
        changed.setFullName(name);
        changed.setPhone(phone);
        changed.setWhatsapp(whatsapp);
        changed.setAddress(address.isEmpty() ? null : address);
        userDAO.updateContact(changed);
        return userDAO.findById(current.getId());
    }

    public void changePassword(User user, String currentPassword, String newPassword, String confirm)
            throws SQLException, ValidationException {
        if (!PasswordUtil.matches(currentPassword, userDAO.findPasswordHash(user.getId()))) {
            throw new ValidationException("Your current password is not correct.");
        }
        List<String> errors = new ArrayList<>();
        checkNewPassword(newPassword, confirm, errors);
        if (errors.isEmpty() && newPassword.equals(currentPassword)) {
            errors.add("The new password must be different from the current one.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        userDAO.updatePassword(user.getId(), PasswordUtil.hash(newPassword));
    }

    /**
     * Saves a new profile photo (replacing the old one) and returns the fresh user.
     *
     * @param data the file (the servlet reads at most MAX_PHOTO_BYTES + 1 bytes)
     */
    public User changePhoto(User user, byte[] data) throws SQLException, ValidationException {
        if (data == null || data.length == 0) {
            throw new ValidationException("Please choose a photo (JPG or PNG).");
        }
        if (data.length > MAX_PHOTO_BYTES) {
            throw new ValidationException("The photo is too large. The limit is 2 MB.");
        }
        // The real type comes from the first bytes, not from the file name.
        String type = PrescriptionService.detectType(data);
        if (!"image/jpeg".equals(type) && !"image/png".equals(type)) {
            throw new ValidationException("Only JPG or PNG photos can be used.");
        }

        String key;
        try {
            key = StorageFactory.getStorage().save(new ByteArrayInputStream(data), PHOTO_FOLDER,
                    type.equals("image/png") ? "png" : "jpg", type);
        } catch (IOException e) {
            throw new SQLException("Could not save the photo", e);
        }
        String oldKey = userDAO.findById(user.getId()).getPhotoKey();
        userDAO.updatePhoto(user.getId(), key);
        deletePhotoFile(oldKey);
        return userDAO.findById(user.getId());
    }

    public User removePhoto(User user) throws SQLException {
        String oldKey = userDAO.findById(user.getId()).getPhotoKey();
        userDAO.updatePhoto(user.getId(), null);
        deletePhotoFile(oldKey);
        return userDAO.findById(user.getId());
    }

    private void deletePhotoFile(String key) {
        // Demo photos ("samples/...") are shared files and stay.
        if (key == null || key.startsWith("samples/")) {
            return;
        }
        try {
            StorageFactory.getStorage().delete(key);
        } catch (IOException e) {
            System.out.println("[MediSys] Could not delete the old photo " + key + ": " + e.getMessage());
        }
    }

    /**
     * The user whose photo the viewer may see, or null.
     * Everyone sees their own photo; pharmacists and admins see all of them.
     */
    public User getPhotoOwner(User viewer, int userId) throws SQLException {
        if (viewer.getId() != userId && !viewer.isPharmacist() && !viewer.isAdmin()) {
            return null;
        }
        User owner = userDAO.findById(userId);
        return owner == null || !owner.hasPhoto() ? null : owner;
    }

    public InputStream openPhoto(User owner) throws IOException {
        return StorageFactory.getStorage().open(owner.getPhotoKey());
    }

    // ===================================================== pharmacist view

    /** Prescriptions, rejected prescriptions and orders of a customer. */
    public Map<String, Integer> getCustomerStats(int userId) throws SQLException {
        return userDAO.customerStats(userId);
    }

    /** A pharmacist or admin puts a red flag on a customer. */
    public String flag(User staff, int customerId, String reasonText) throws SQLException, ValidationException {
        User customer = checkFlagAllowed(staff, customerId);
        String reason = TextUtil.clean(reasonText);
        if (reason.length() < FLAG_REASON_MIN || reason.length() > FLAG_REASON_MAX) {
            throw new ValidationException("Please write why this customer is flagged (" + FLAG_REASON_MIN
                    + " to " + FLAG_REASON_MAX + " characters), so other staff know.");
        }
        if (customer.isFlagged()) {
            throw new ValidationException(customer.getFullName() + " is already flagged.");
        }
        userDAO.flag(customerId, reason, staff.getId());
        return customer.getFullName() + " is now flagged in red. They can still use MediSys.";
    }

    public String clearFlag(User staff, int customerId) throws SQLException, ValidationException {
        User customer = checkFlagAllowed(staff, customerId);
        if (!userDAO.clearFlag(customerId)) {
            throw new ValidationException(customer.getFullName() + " is not flagged.");
        }
        return "The red flag was removed from " + customer.getFullName() + ".";
    }

    private User checkFlagAllowed(User staff, int customerId) throws SQLException, ValidationException {
        if (!staff.isPharmacist() && !staff.isAdmin()) {
            throw new ValidationException("Only pharmacists and administrators can flag customers.");
        }
        User customer = userDAO.findById(customerId);
        if (customer == null || !customer.isCustomer()) {
            throw new ValidationException("That customer was not found.");
        }
        return customer;
    }

    // ============================================================== admin

    /** @param filter one of the UserDAO.FILTER_ values */
    public List<User> getUsers(String filter, String keyword) throws SQLException {
        return userDAO.findForAdmin(filter, TextUtil.clean(keyword));
    }

    public Map<String, Integer> getCounts() throws SQLException {
        return userDAO.countForAdmin();
    }

    /** The admin adds a staff account (pharmacist, delivery staff or admin). */
    public User createStaff(User admin, Map<String, String> form, String password)
            throws SQLException, ValidationException {
        checkAdmin(admin);
        List<String> errors = new ArrayList<>();
        String name = checkName(form.get("fullName"), errors);
        String email = TextUtil.clean(form.get("email")).toLowerCase();
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}") || email.length() > 150) {
            errors.add("Please enter a valid email address.");
        } else if (userDAO.emailTaken(email, 0)) {
            errors.add("An account with this email already exists.");
        }
        String phone = checkPhone(form.get("phone"), "phone number", false, false, errors);
        Role role = Role.fromText(form.get("role"));
        if (role == Role.CUSTOMER || !String.valueOf(form.get("role")).equalsIgnoreCase(role.name())) {
            errors.add("Please choose the staff role.");
        }
        checkNewPassword(password, password, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress("MediSys Pharmacy");
        user.setRole(role);
        return userDAO.findById(userDAO.create(user, PasswordUtil.hash(password)));
    }

    /** Switches a staff account on or off. Customers are never switched off (they get a red flag instead). */
    public String setActive(User admin, int id, boolean active) throws SQLException, ValidationException {
        checkAdmin(admin);
        User user = userDAO.findById(id);
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

    private void checkAdmin(User user) throws ValidationException {
        if (!user.isAdmin()) {
            throw new ValidationException("Only an administrator can do this.");
        }
    }

    // ========================================================== checks

    private String checkName(String text, List<String> errors) {
        String name = TextUtil.clean(text).replaceAll("\\s+", " ");
        if (name.length() < 2 || name.length() > 100 || !name.matches("[\\p{L} .'-]+")) {
            errors.add("Please enter your full name (letters only, 2 to 100 characters).");
        }
        return name;
    }

    /**
     * Checks a Sri Lankan phone number and returns it as 0771234567.
     * +94 771234567, 077-123 4567 and similar are accepted.
     *
     * @param required   an empty value is an error
     * @param mobileOnly must be a mobile number (07...), e.g. for WhatsApp
     * @return the number, or null when it is empty
     */
    private String checkPhone(String text, String label, boolean required, boolean mobileOnly, List<String> errors) {
        String digits = TextUtil.clean(text).replaceAll("[\\s\\-()]", "");
        if (digits.isEmpty()) {
            if (required) {
                errors.add("Please enter your " + label + ".");
            }
            return null;
        }
        if (digits.startsWith("+94")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("94") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        String pattern = mobileOnly ? "07\\d{8}" : "0\\d{9}";
        if (!digits.matches(pattern)) {
            errors.add("Please enter a valid " + label + (mobileOnly ? " (a mobile number, e.g. 0771234567)." : ", e.g. 0771234567."));
            return null;
        }
        return digits;
    }

    private void checkNewPassword(String password, String confirm, List<String> errors) {
        if (password == null || password.length() < PASSWORD_MIN || password.length() > 100
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            errors.add("The password needs at least " + PASSWORD_MIN + " characters, with at least one letter and one number.");
        } else if (!password.equals(confirm)) {
            errors.add("The two passwords do not match.");
        }
    }
}
