package com.medisys.util;

import com.medisys.config.DBConnection;
import com.medisys.storage.FileStorage;
import com.medisys.storage.StorageFactory;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.InputStream;
import java.sql.Connection;
import java.util.Set;

/**
 * Runs once when Tomcat starts the app:
 *  - prints whether the database can be reached, so a wrong db.properties is
 *    noticed straight away in the console
 *  - prepares the file storage and copies the demo prescription files
 *    (WEB-INF/sample-uploads) into it, so the rows in sample-data.sql have
 *    real files behind them
 * When the app stops, it closes the database connection pool.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
@WebListener
public class AppInitListener implements ServletContextListener {

    private static final String SAMPLE_FOLDER = "/WEB-INF/sample-uploads/";

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (Connection con = DBConnection.getInstance().getConnection()) {
            System.out.println("[MediSys] Database connected: " + con.getMetaData().getURL());
        } catch (Exception e) {
            // The app still starts, but pages that use the database will show an error.
            System.out.println("[MediSys] DATABASE NOT CONNECTED: " + e.getMessage());
        }

        try {
            FileStorage storage = StorageFactory.getStorage();
            System.out.println("[MediSys] File storage: " + storage.describe());
            copySampleFiles(event.getServletContext(), storage);
        } catch (Exception e) {
            System.out.println("[MediSys] FILE STORAGE NOT READY: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // Close the pooled database connections when Tomcat stops the app.
        DBConnection.shutdown();
    }

    /** Copies each demo file to "samples/<name>" unless it is already there. */
    private void copySampleFiles(ServletContext context, FileStorage storage) throws Exception {
        Set<String> paths = context.getResourcePaths(SAMPLE_FOLDER);
        if (paths == null) {
            return;
        }
        for (String path : paths) {
            String name = path.substring(SAMPLE_FOLDER.length());
            String key = "samples/" + name;
            if (storage.exists(key)) {
                continue;
            }
            String type = name.endsWith(".pdf") ? "application/pdf"
                    : name.endsWith(".png") ? "image/png" : "image/jpeg";
            try (InputStream in = context.getResourceAsStream(path)) {
                storage.put(key, in, type);
            }
        }
    }
}
