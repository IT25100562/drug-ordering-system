package com.medisys.filter;

import com.medisys.model.Role;
import com.medisys.model.User;
import com.medisys.util.JsonUtil;
import com.medisys.util.SessionUtil;
import com.medisys.util.TextUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Guards pages that need a login, and pages that need a specific role.
 *
 *   not logged in      -> sent to /login (and back here after logging in)
 *   wrong role         -> sent to the home page with a message
 *
 * Which role may open which path is decided in allowedRoles() below.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
@WebFilter(urlPatterns = {"/account/*", "/cart/*", "/wishlist/*", "/checkout/*", "/orders/*",
        "/prescriptions/*", "/pharmacist/*", "/admin/*", "/staff/*", "/deliveries/*", "/notifications", "/notifications/*", "/users/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI().substring(request.getContextPath().length());
        User user = SessionUtil.currentUser(request);

        if (user == null) {
            if (JsonUtil.wantsJson(request)) {
                JsonUtil.write(response, HttpServletResponse.SC_UNAUTHORIZED, Map.of(
                        "ok", false,
                        "message", "Please log in first.",
                        "loginUrl", request.getContextPath() + "/login"));
                return;
            }
            // Come back here after login: the page itself for a page view, or
            // the page the form was on (its "returnTo" field) for a form post.
            String back;
            if ("GET".equals(request.getMethod())) {
                back = request.getQueryString() == null ? path : path + "?" + request.getQueryString();
            } else {
                back = request.getParameter("returnTo");
            }
            if (!TextUtil.isSafeLocalPath(back)) {
                back = "";
            }
            // Guests who want to upload a prescription are told they need an account.
            SessionUtil.flash(request, "info", path.startsWith("/prescriptions")
                    ? "Only registered customers can upload prescriptions. Please log in, or create a free account."
                    : "Please log in to continue.");
            response.sendRedirect(request.getContextPath() + "/login"
                    + (back.isEmpty() ? "" : "?returnTo=" + URLEncoder.encode(back, StandardCharsets.UTF_8)));
            return;
        }

        Role[] allowed = allowedRoles(path);
        if (!isOneOf(user.getRole(), allowed)) {
            if (JsonUtil.wantsJson(request)) {
                JsonUtil.write(response, HttpServletResponse.SC_FORBIDDEN, Map.of(
                        "ok", false, "message", "Your account cannot do this."));
                return;
            }
            SessionUtil.flash(request, "error", "That page is not available for your account.");
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        chain.doFilter(request, response);
    }

    /** The roles that may open a path. Any logged in user may open the rest. */
    private Role[] allowedRoles(String path) {
        if (path.startsWith("/admin")) {
            return new Role[]{Role.ADMIN};
        }
        if (path.startsWith("/pharmacist")) {
            return new Role[]{Role.PHARMACIST};
        }
        if (path.equals("/prescriptions/file")) {
            // The servlet itself checks that a customer only gets their own file.
            return new Role[]{Role.CUSTOMER, Role.PHARMACIST};
        }
        if (path.equals("/users/flag")) {
            return new Role[]{Role.PHARMACIST, Role.ADMIN};
        }
        if (path.startsWith("/staff")) {
            return new Role[]{Role.DELIVERY_STAFF, Role.ADMIN};
        }
        if (path.startsWith("/cart") || path.startsWith("/wishlist") || path.startsWith("/checkout")
                || path.startsWith("/orders") || path.startsWith("/prescriptions")
                || path.startsWith("/deliveries")) {
            return new Role[]{Role.CUSTOMER};
        }
        return Role.values();
    }

    private boolean isOneOf(Role role, Role[] roles) {
        for (Role r : roles) {
            if (r == role) {
                return true;
            }
        }
        return false;
    }
}
