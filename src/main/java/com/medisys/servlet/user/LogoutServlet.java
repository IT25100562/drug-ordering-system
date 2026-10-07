package com.medisys.servlet.user;

import com.medisys.util.SessionUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Logs the user out and goes back to the login page.
 *
 *   POST /logout
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SessionUtil.logout(request);
        SessionUtil.flash(request, "success", "You have been logged out.");
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
