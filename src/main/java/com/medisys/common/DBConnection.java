package com.medisys.common;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Single place that gives out JDBC connections to the PostgreSQL database
 * (Supabase in the live system, or a local PostgreSQL on a laptop).
 *
 * Design pattern: SINGLETON. There is only ever one DBConnection object
 * (getInstance()), so the settings are read once and every DAO shares one
 * connection pool.
 *
 * Connection pool: opening a new connection to the cloud database takes a
 * few hundred ms, and one page can need several. So a few connections are kept
 * open in a pool (Tomcat's built-in DBCP) and lent out again and again.
 * Calling close() on a pooled connection does not really close it - it goes
 * back to the pool.
 *
 * Always use try-with-resources, so every connection is given back:
 *
 *     try (Connection con = DBConnection.getInstance().getConnection();
 *          PreparedStatement ps = con.prepareStatement(sql)) { ... }
 *
 * Settings (see AppConfig): db.url, db.user, db.password
 * (on the live server: the DB_URL, DB_USER and DB_PASSWORD environment variables).
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public final class DBConnection {

    private static DBConnection instance;

    private final BasicDataSource pool;

    private DBConnection() {
        if (!AppConfig.has("db.url")) {
            throw new IllegalStateException(
                    "No database settings. Copy src/main/resources/db.properties.example to "
                    + "db.properties (or set the DB_URL, DB_USER and DB_PASSWORD environment variables).");
        }
        pool = new BasicDataSource();
        pool.setUrl(AppConfig.get("db.url"));
        pool.setUsername(AppConfig.get("db.user"));
        pool.setPassword(AppConfig.get("db.password"));
        // The driver jar is inside our app (WEB-INF/lib), which the pool cannot
        // see by itself, so we hand it a driver object.
        pool.setDriver(new org.postgresql.Driver());
        pool.setInitialSize(1);
        pool.setMaxTotal(8);            // the free Supabase plan allows only a few connections
        pool.setMaxIdle(4);             // keep up to 4 ready when the site is quiet
        pool.setValidationQuery("SELECT 1");
        pool.setTestOnBorrow(true);     // replace connections the server has dropped
    }

    /** Returns the one and only DBConnection object. */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /** Borrows a connection from the pool. The caller must close() it to give it back. */
    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }

    /** Closes every pooled connection. Called when the app stops. */
    public static synchronized void shutdown() {
        if (instance != null) {
            try {
                instance.pool.close();
            } catch (SQLException e) {
                System.out.println("[MediSys] Could not close the connection pool: " + e.getMessage());
            }
            instance = null;
        }
    }
}
