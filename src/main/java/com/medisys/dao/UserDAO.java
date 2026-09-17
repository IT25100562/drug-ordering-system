package com.medisys.dao;

import com.medisys.model.User;

import java.sql.SQLException;

/**
 * Database operations for users.
 *
 * Only what login needs is here so far (added early so the cart can work).
 *
 * Module : 04 - User and Role Management
 * Owner  : Kaweesha P. M. G. S.
 *
 * TODO: findAll, create, update, updatePassword, setActive, changeRole.
 */
public interface UserDAO {

    /** Returns the user, or null when the id does not exist. */
    User findById(int id) throws SQLException;

    /** Returns the user, or null when no user has this email. */
    User findByEmail(String email) throws SQLException;

    /** The stored password hash for this user id, or null. */
    String findPasswordHash(int id) throws SQLException;
}
