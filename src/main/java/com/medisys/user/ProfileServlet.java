package com.medisys.user;

import com.medisys.common.FileStorage;
import com.medisys.common.PasswordUtil;
import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.Validator;
import com.medisys.order.OrderDAO;
import com.medisys.prescription.PrescriptionDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The user's own profile (READ and UPDATE of their account).
 *
 *   GET  /account/profile                       the profile page (+ recent prescriptions and orders)
 *   POST /account/profile  action=details       change name, phone, WhatsApp, address
 *   POST /account/profile  action=password      change the password
 *   POST /account/photo    (file "photo")       upload a new profile photo
 *   POST /account/photo    action=remove        delete the profile photo
 *   GET  /users/photo?id=5                      show a profile photo
 *
 * NIC, date of birth and email identify the person, so they can't be changed online.
 * Profile photo: JPG or PNG, at most 2 MB. Only the user, pharmacists and admins see it.
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet({"/account/profile", "/account/photo", "/users/photo"})
@MultipartConfig(maxFileSize = 5L * 1024 * 1024, maxRequestSize = 6L * 1024 * 1024)
public class ProfileServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/user/profile.jsp";
    private static final int MAX_PHOTO_BYTES = 2 * 1024 * 1024;
    private static final int RECENT = 5;

    private final UserDAO userDAO = new UserDAO();
    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/users/photo")) {
                showPhoto(request, response);
                return;
            }
            // READ: always load fresh from the database.
            User user = userDAO.getUserById(SessionUtil.currentUser(request).getId());
            SessionUtil.refreshUser(request, user);
            showPage(request, response, user, null, null, null);
        } catch (SQLException e) {
            throw new ServletException("Could not load the profile", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/account/photo")) {
                changePhoto(request, response);
                return;
            }
            User user = SessionUtil.currentUser(request);
            String action = TextUtil.clean(request.getParameter("action"));
            if ("details".equals(action)) {
                updateDetails(request, response, user);
            } else if ("password".equals(action)) {
                changePassword(request, response, user);
            } else {
                response.sendRedirect(request.getContextPath() + "/account/profile");
            }
        } catch (SQLException e) {
            throw new ServletException("Could not save the profile", e);
        }
    }

    // ================================================================ UPDATE

    /** Saves name, phone, WhatsApp and address. */
    private void updateDetails(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        Map<String, String> form = new HashMap<>();
        for (String field : new String[]{"fullName", "phone", "whatsapp", "address"}) {
            form.put(field, request.getParameter(field));
        }

        // ---- validation
        List<String> errors = new ArrayList<>();
        String name = Validator.name(form.get("fullName"), errors);
        String phone = Validator.phone(form.get("phone"), "phone number", user.isCustomer(), false, errors);
        String whatsapp = Validator.phone(form.get("whatsapp"), "WhatsApp number", false, true, errors);
        String address = TextUtil.clean(form.get("address"));
        if (address.length() > 255) {
            errors.add("The address can have at most 255 characters.");
        } else if (!address.isEmpty() && address.length() < 5) {
            errors.add("Please enter the full address (or leave it empty).");
        }
        if (!errors.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showPage(request, response, userDAO.getUserById(user.getId()), "details", errors, form);
            return;
        }

        User changed = userDAO.getUserById(user.getId());
        changed.setFullName(name);
        changed.setPhone(phone);
        changed.setWhatsapp(whatsapp);
        changed.setAddress(address.isEmpty() ? null : address);
        userDAO.updateContactDetails(changed);

        SessionUtil.refreshUser(request, userDAO.getUserById(user.getId()));
        SessionUtil.flash(request, "success", "Your details were saved.");
        response.sendRedirect(request.getContextPath() + "/account/profile");
    }

    private void changePassword(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        String current = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");

        // ---- validation
        List<String> errors = new ArrayList<>();
        if (!PasswordUtil.matches(current, userDAO.getPasswordHash(user.getId()))) {
            errors.add("Your current password is not correct.");
        } else {
            Validator.password(newPassword, request.getParameter("confirmPassword"), errors);
            if (errors.isEmpty() && newPassword.equals(current)) {
                errors.add("The new password must be different from the current one.");
            }
        }
        if (!errors.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showPage(request, response, userDAO.getUserById(user.getId()), "password", errors, new HashMap<>());
            return;
        }

        userDAO.updatePassword(user.getId(), PasswordUtil.hash(newPassword));
        SessionUtil.flash(request, "success", "Your password was changed.");
        response.sendRedirect(request.getContextPath() + "/account/profile");
    }

    /** Uploads a new photo (replacing the old one), or removes it. */
    private void changePhoto(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        String oldKey = userDAO.getUserById(user.getId()).getPhotoKey();
        String error = null;

        try {
            request.getParts();     // reads the whole body, so a too-large file is noticed here
        } catch (IllegalStateException e) {
            error = "The photo is too large. The limit is 2 MB.";
        }

        if (error == null && "remove".equals(request.getParameter("action"))) {
            // DELETE the photo
            userDAO.updatePhoto(user.getId(), null);
            deletePhotoFile(oldKey);
            SessionUtil.flash(request, "success", "Your photo was removed.");
        } else if (error == null) {
            byte[] data = readPhoto(request);
            // ---- validation: the real type comes from the first bytes, not from the file name
            String type = FileStorage.detectType(data);
            if (data.length == 0) {
                error = "Please choose a photo (JPG or PNG).";
            } else if (data.length > MAX_PHOTO_BYTES) {
                error = "The photo is too large. The limit is 2 MB.";
            } else if (!"image/jpeg".equals(type) && !"image/png".equals(type)) {
                error = "Only JPG or PNG photos can be used.";
            } else {
                String key = FileStorage.save(data, "profiles", type);
                userDAO.updatePhoto(user.getId(), key);
                deletePhotoFile(oldKey);
                SessionUtil.flash(request, "success", "Your new photo was saved.");
            }
        }
        if (error != null) {
            SessionUtil.flash(request, "error", error);
        }
        SessionUtil.refreshUser(request, userDAO.getUserById(user.getId()));
        response.sendRedirect(request.getContextPath() + "/account/profile");
    }

    // ================================================================== READ

    /** Sends a profile photo. Everyone sees their own; pharmacists and admins see all of them. */
    private void showPhoto(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        User viewer = SessionUtil.currentUser(request);
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        User owner = null;
        if (id != null && (viewer.getId() == id || viewer.isPharmacist() || viewer.isAdmin())) {
            owner = userDAO.getUserById(id);
        }
        if (owner == null || !owner.hasPhoto()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try (InputStream in = FileStorage.open(owner.getPhotoKey())) {
            // Only JPG / PNG are accepted at upload, so the key's extension tells the type.
            response.setContentType(owner.getPhotoKey().endsWith(".png") ? "image/png" : "image/jpeg");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "private, max-age=300");
            in.transferTo(response.getOutputStream());
        } catch (NoSuchFileException e) {
            log("Profile photo missing: " + owner.getPhotoKey());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response, User user,
                          String errorForm, List<String> errors, Map<String, String> form)
            throws SQLException, ServletException, IOException {
        request.setAttribute("profile", user);
        request.setAttribute("errorForm", errorForm);     // which form the errors belong to
        request.setAttribute("errors", errors);
        request.setAttribute("form", form);
        if (user.isCustomer()) {
            request.setAttribute("prescriptions", newest(prescriptionDAO.getPrescriptionsByUser(user.getId())));
            request.setAttribute("orders", newest(orderDAO.getOrdersByUser(user.getId())));
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    // =============================================================== helpers

    /** The newest few (the lists are already newest first). */
    private static <T> List<T> newest(List<T> list) {
        return list.size() > RECENT ? list.subList(0, RECENT) : list;
    }

    /** Reads at most MAX_PHOTO_BYTES + 1 bytes: enough to notice a file that is too big. */
    private byte[] readPhoto(HttpServletRequest request) throws IOException, ServletException {
        Part part = request.getPart("photo");
        if (part == null || part.getSize() == 0) {
            return new byte[0];
        }
        try (InputStream in = part.getInputStream()) {
            return in.readNBytes(MAX_PHOTO_BYTES + 1);
        } finally {
            part.delete();
        }
    }

    private void deletePhotoFile(String key) {
        // Demo photos ("samples/...") are shared files and stay.
        if (key != null && !key.startsWith("samples/")) {
            FileStorage.delete(key);
        }
    }
}
