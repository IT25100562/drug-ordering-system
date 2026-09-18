package com.medisys.dao;

import com.medisys.model.SavedReport;

import java.sql.SQLException;
import java.util.List;

/**
 * Database operations for saved reports (create, read, update, delete).
 *
 * Module : 04 - Reports and Analytics
 * Owner  : Kaweesha P. M. G. S.
 */
public interface SavedReportDAO {

    /** Saves a new snapshot and returns its id. */
    int create(SavedReport report) throws SQLException;

    /** The saved report, or null. */
    SavedReport findById(int id) throws SQLException;

    /** All saved reports, newest period first. */
    List<SavedReport> findAll() throws SQLException;

    /** Changes the title and notes (the numbers are never changed). */
    boolean update(int id, String title, String notes) throws SQLException;

    boolean delete(int id) throws SQLException;
}
