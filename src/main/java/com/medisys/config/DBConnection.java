package com.medisys.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single place that opens JDBC connections to SQL Server.
 *
 * Design pattern: SINGLETON. There is only ever one DBConnection object
 * (getInstance()), so db.properties is read once and every DAO uses the same
 * settings.
 *
 * Every call to getConnection() gives a NEW connection. Always close it with
 * try-with-resources:
 *
 *     try (Connection con = DBConnection.getInstance().getConnection();
 *          PreparedStatement ps = con.prepareStatement(sql)) { ... }
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public final class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";

    private static DBConnection instance;

    private final String url;
    private final String user;
    private final String password;

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

        url = props.getProperty("db.url");
        user = props.getProperty("db.user");
        password = props.getProperty("db.password");

        try {
            // Tomcat does not always auto-load JDBC drivers from WEB-INF/lib,
            // so load the SQL Server driver by name.
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SQL Server JDBC driver not found", e);
        }
    }

    /** Returns the one and only DBConnection object. */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /** Opens a new connection. The caller must close it. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
