package com.medisys.servlet.prescription;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

/**
 * Streams an uploaded file after checking who is asking.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 *
 * TODO: owner or pharmacist only, otherwise 404.
 */
@WebServlet("/prescriptions/file")
public class PrescriptionFileServlet extends HttpServlet {
}
