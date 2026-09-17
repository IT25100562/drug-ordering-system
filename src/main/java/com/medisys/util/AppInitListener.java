package com.medisys.util;

import com.medisys.config.DBConnection;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;

/**
 * Runs once when Tomcat starts the app and prints whether the database can be
 * reached, so a wrong db.properties is noticed straight away in the console.
 * When the app stops, it closes the database connection pool.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 *
 * TODO (module 05): create the uploads folder here.
 */
@WebListener
public class AppInitListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            System.out.println("[MediSys] Database connected: " + con.getMetaData().getURL());
        } catch (Exception e) {
            // The app still starts, but pages that use the database will show an error.
            System.out.println("[MediSys] DATABASE NOT CONNECTED: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Close the pooled database connections when Tomcat stops the app.
        DBConnection.shutdown();
    }
}
