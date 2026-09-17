package com.medisys.dao;

import com.medisys.model.Prescription;
import com.medisys.model.PrescriptionItem;
import com.medisys.model.PrescriptionStatus;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Database operations for prescriptions and their medicine lines.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
public interface PrescriptionDAO {

    /** Dashboard filters, besides PENDING, CORRECTION_REQUESTED and REJECTED. */
    String FILTER_APPROVED = "APPROVED";   // approved, waiting for the customer to pay
    String FILTER_PAID = "PAID";
    String FILTER_EXPIRED = "EXPIRED";
    String FILTER_ALL = "ALL";

    /** Results of pay(). */
    int PAY_OK = 0;
    int PAY_NOT_PAYABLE = -1;               // any positive number = id of a medicine without enough stock

    /** Inserts a new PENDING prescription and returns its id. */
    int create(Prescription prescription) throws SQLException;

    /** Returns the prescription (with customer and medicine lines), or null. */
    Prescription findById(int id) throws SQLException;

    /** All prescriptions of one customer, newest first. */
    List<Prescription> findByUser(int userId) throws SQLException;

    /** @param filter a status name, or one of the FILTER_ values */
    List<Prescription> findForDashboard(String filter) throws SQLException;

    /** How many prescriptions each dashboard filter shows (filter -> count). */
    Map<String, Integer> countForDashboard() throws SQLException;

    /** How many of the customer's prescriptions are still waiting (pending or correction). */
    int countOpen(int userId) throws SQLException;

    /**
     * Approves a PENDING prescription and saves its medicine lines, in one
     * transaction. Returns false if it was no longer PENDING.
     */
    boolean approve(int id, String note, int reviewerId, List<PrescriptionItem> items) throws SQLException;

    /** Rejects or asks for a correction, only if the prescription is still PENDING. */
    boolean decide(int id, PrescriptionStatus status, String note, int reviewerId) throws SQLException;

    /** Puts a corrected copy in place and sets the status back to PENDING. */
    boolean replaceFile(int id, String fileKey, String originalFileName, String contentType, int fileSize)
            throws SQLException;

    /** Deletes the prescription (and its lines), only if it was never paid. */
    boolean delete(int id) throws SQLException;

    /**
     * Records the payment and takes the medicines out of stock, all in one
     * transaction (either everything happens or nothing does).
     *
     * @return PAY_OK, PAY_NOT_PAYABLE, or the id of a medicine that is short in stock
     */
    int pay(Prescription prescription, String paymentReference, String cardLast4,
            String deliveryName, String deliveryAddress, String deliveryPhone) throws SQLException;
}
