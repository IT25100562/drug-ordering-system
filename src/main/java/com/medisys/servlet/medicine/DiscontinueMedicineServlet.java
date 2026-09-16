package com.medisys.servlet.medicine;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

/**
 * Admin marks a medicine as discontinued (soft delete).
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 *
 * TODO: doPost only, then redirect to /admin/medicines.
 */
@WebServlet("/admin/medicines/discontinue")
public class DiscontinueMedicineServlet extends HttpServlet {
}
