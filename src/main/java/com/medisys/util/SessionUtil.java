package com.medisys.util;

import com.medisys.model.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Reads and writes things kept in the HTTP session:
 *
 *  - the logged in user (currentUser / login / logout)
 *  - flash messages: a one-time message (for example "Medicine saved") stored
 *    just before a redirect and shown once on the next page
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public final class SessionUtil {

    private static final String USER_KEY = "loggedInUser";

    private SessionUtil() {
    }

    /** The logged in user, or null when nobody is logged in. */
    public static User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(USER_KEY);
        return value instanceof User ? (User) value : null;
    }

    public static void login(HttpServletRequest request, User user) {
        // A new session id after login protects against session fixation.
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        request.getSession(true).setAttribute(USER_KEY, user);
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** Keeps a message for the next page. type is "success", "error" or "info". */
    public static void flash(HttpServletRequest request, String type, String message) {
        request.getSession(true).setAttribute("flash_" + type, message);
    }

    /** Returns the stored message once and removes it (null when there is none). */
    public static String takeFlash(HttpServletRequest request, String type) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute("flash_" + type);
        session.removeAttribute("flash_" + type);
        return value == null ? null : value.toString();
    }
}
