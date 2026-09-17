package com.medisys.servlet.user;

import com.medisys.model.User;
import com.medisys.service.UserService;
import com.medisys.util.SessionUtil;
import com.medisys.util.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.sql.SQLException;

/**
 * Sends a profile photo.
 *
 *   GET /users/photo?id=3
 *
 * Users get their own photo; pharmacists and admins get anyone's.
 * Everyone else (and a user without a photo) gets 404.
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/users/photo")
public class UserPhotoServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        User owner;
        try {
            owner = id == null ? null : userService.getPhotoOwner(SessionUtil.currentUser(request), id);
        } catch (SQLException e) {
            throw new ServletException("Could not load the photo", e);
        }
        if (owner == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (InputStream in = userService.openPhoto(owner)) {
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
}
