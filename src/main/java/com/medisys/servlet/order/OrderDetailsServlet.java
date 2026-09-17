package com.medisys.servlet.order;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

/**
 * One order with its items.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 *
 * TODO: load by ?id= (owner or admin only).
 */
@WebServlet("/orders/view")
public class OrderDetailsServlet extends HttpServlet {
}
