package com.medisys.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Reads and writes things kept in the HTTP session.
 *
 * Flash messages: a one-time message (for example "Medicine saved") stored just
 * before a redirect and shown once on the next page.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 *
 * TODO (module 04): currentUser(), login(), logout() once User exists.
 */
public final class SessionUtil {

    private SessionUtil() {
    }

    /** Keeps a message for the next page. type is "success" or "error". */
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
