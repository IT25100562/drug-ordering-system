package com.medisys.servlet.user;

import com.medisys.model.User;
import com.medisys.service.UserService;
import com.medisys.service.ValidationException;
import com.medisys.util.SessionUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

/**
 * Changes or removes the logged in user's profile photo.
 *
 *   POST /account/photo   (multipart) photo=<file>
 *   POST /account/photo   action=remove
 *
 * The Tomcat limit is above 2 MB so UserService can give a friendly
 * "too large" message.
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/account/photo")
@MultipartConfig(maxFileSize = 5L * 1024 * 1024, maxRequestSize = 6L * 1024 * 1024)
public class ProfilePhotoServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            User changed;
            if ("remove".equals(readAction(request))) {
                changed = userService.removePhoto(user);
                SessionUtil.flash(request, "success", "Your photo was removed.");
            } else {
                changed = userService.changePhoto(user, readPhoto(request));
                SessionUtil.flash(request, "success", "Your new photo was saved.");
            }
            SessionUtil.refreshUser(request, changed);
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Could not change the photo", e);
        }
        response.sendRedirect(request.getContextPath() + "/account/profile");
    }

    private String readAction(HttpServletRequest request) throws ValidationException, IOException, ServletException {
        try {
            request.getParts();     // reads the whole body, so a too-large file is noticed here
        } catch (IllegalStateException e) {
            throw new ValidationException("The photo is too large. The limit is 2 MB.");
        }
        return request.getParameter("action");
    }

    /** Reads at most MAX_PHOTO_BYTES + 1 bytes: enough to notice a file that is too big. */
    private byte[] readPhoto(HttpServletRequest request) throws IOException, ServletException {
        Part part = request.getPart("photo");
        if (part == null || part.getSize() == 0) {
            return new byte[0];
        }
        try (InputStream in = part.getInputStream()) {
            return in.readNBytes(UserService.MAX_PHOTO_BYTES + 1);
        } finally {
            part.delete();
        }
    }
}
