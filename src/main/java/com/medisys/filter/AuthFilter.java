package com.medisys.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

import java.io.IOException;

/**
 * Guards pages that need a login, and pages that need a specific role.
 *
 * Module : Shared - agree with the team before changing
 * Owner  : Whole team
 *
 * TODO: redirect to /login when nobody is logged in; /admin/* only ADMIN, /pharmacist/* only PHARMACIST, /staff/* only DELIVERY_STAFF.
 */
@WebFilter(urlPatterns = {"/account/*", "/cart/*", "/wishlist/*", "/checkout/*", "/orders/*", "/prescriptions/*", "/pharmacist/*", "/admin/*", "/staff/*", "/deliveries/*", "/notifications"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // TODO: real checks go here. For now every request is let through.
        chain.doFilter(request, response);
    }
}
