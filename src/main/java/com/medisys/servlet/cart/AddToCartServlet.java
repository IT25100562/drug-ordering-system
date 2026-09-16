package com.medisys.servlet.cart;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

/**
 * Adds a medicine to the cart.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 *
 * TODO: doPost; if the medicine needs a prescription, send the customer to /prescriptions/upload.
 */
@WebServlet("/cart/add")
public class AddToCartServlet extends HttpServlet {
}
