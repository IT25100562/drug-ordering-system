package com.medisys.servlet.user;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

/**
 * Logs the user out.
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 *
 * TODO: invalidate the session and redirect to /login.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
}
