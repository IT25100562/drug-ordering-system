package com.medisys.config;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single place that gives out JDBC connections to SQL Server.
 *
 * Design pattern: SINGLETON. There is only ever one DBConnection object
 * (getInstance()), so db.properties is read once and every DAO shares it.
 *
 * Connection pool: opening a new SQL Server connection takes about 250 ms, and
 * one page can need several. So a few connections are kept open in a pool
 * (Tomcat's built-in DBCP) and lent out again and again. Calling close() on a
 * pooled connection does not really close it - it goes back to the pool.
 *
 * Always use try-with-resources, so every connection is given back:
 *
 *     try (Connection con = DBConnection.getInstance().getConnection();
 *          PreparedStatement ps = con.prepareStatement(sql)) { ... }
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public final class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";
    private static final String DRIVER_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private static DBConnection instance;

    private final BasicDataSource pool;

    private DBConnection() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "src/main/resources/db.properties is missing. "
                        + "Copy db.properties.example to db.properties and rebuild.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + PROPERTIES_FILE, e);
        }

        pool = new BasicDataSource();
        pool.setUrl(props.getProperty("db.url"));
        pool.setUsername(props.getProperty("db.user"));
        pool.setPassword(props.getProperty("db.password"));
        // The driver jar is inside our app (WEB-INF/lib), which the pool cannot
        // see by itself, so we hand it a driver object.
        pool.setDriver(loadDriver());
        pool.setInitialSize(2);
        pool.setMaxTotal(20);           // at most 20 open connections
        pool.setMaxIdle(5);             // keep up to 5 ready when the site is quiet
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

    private static Driver loadDriver() {
        try {
            return (Driver) Class.forName(DRIVER_CLASS).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("SQL Server JDBC driver not found", e);
        }
    }
}
