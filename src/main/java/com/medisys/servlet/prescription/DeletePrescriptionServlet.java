package com.medisys.servlet.prescription;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

/**
 * Deletes an invalid or expired prescription and its stored file.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 *
 * TODO: doPost only; refuse when paid.
 */
@WebServlet("/pharmacist/delete")
public class DeletePrescriptionServlet extends HttpServlet {
}
