package com.medisys.servlet.prescription;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

/**
 * Customer sends a corrected copy (only when CORRECTION_REQUESTED).
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 *
 * TODO: add @MultipartConfig; doGet shows correct.jsp, doPost replaces the file.
 */
@WebServlet("/prescriptions/correct")
public class CorrectionUploadServlet extends HttpServlet {
}
