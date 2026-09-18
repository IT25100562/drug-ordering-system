package com.medisys.dao;

import com.medisys.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Database operations for users.
 *
 * Module : Minor functions - User accounts and roles
 * Owner  : Kaweesha P. M. G. S.
 */
public interface UserDAO {

    /** Admin list filters. */
    String FILTER_ALL = "ALL";
    String FILTER_CUSTOMERS = "CUSTOMERS";
    String FILTER_STAFF = "STAFF";
    String FILTER_FLAGGED = "FLAGGED";

    /** Returns the user, or null when the id does not exist. */
    User findById(int id) throws SQLException;

    /** Returns the user, or null when no user has this email. */
    User findByEmail(String email) throws SQLException;

    /** The stored password hash for this user id, or null. */
    String findPasswordHash(int id) throws SQLException;

    /** True when another account (not excludeId) already uses this email. */
    boolean emailTaken(String email, int excludeId) throws SQLException;

    /** True when another account (not excludeId) already uses this NIC. */
    boolean nicTaken(String nic, int excludeId) throws SQLException;

    /** Saves a new user and returns the new id. */
    int create(User user, String passwordHash) throws SQLException;

    /** Saves the details a user may change: name, phone, WhatsApp, address. */
    void updateContact(User user) throws SQLException;

    void updatePassword(int id, String passwordHash) throws SQLException;

    /** Sets (or with null removes) the profile photo key. */
    void updatePhoto(int id, String photoKey) throws SQLException;

    /** Staff accounts only: turn login on or off. */
    boolean setActive(int id, boolean active) throws SQLException;

    /** Marks a customer with a red flag. */
    boolean flag(int id, String reason, int flaggedBy) throws SQLException;

    /** Removes the red flag. */
    boolean clearFlag(int id) throws SQLException;

    /**
     * Users for the admin page, newest first.
     *
     * @param filter  one of the FILTER_ values
     * @param keyword part of the name or email (may be empty)
     */
    List<User> findForAdmin(String filter, String keyword) throws SQLException;

    /** Number of users for each FILTER_ value. */
    Map<String, Integer> countForAdmin() throws SQLException;

    /**
     * A customer's history in numbers, for the pharmacist:
     * "prescriptions", "rejected", "orders".
     */
    Map<String, Integer> customerStats(int userId) throws SQLException;
}
