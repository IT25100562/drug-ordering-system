package com.medisys.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads the app's settings (database login, Cloudinary keys ...).
 *
 * A setting is looked up in this order:
 *   1. an environment variable, e.g. DB_URL    - used on the live server (Render),
 *                                                 where secrets must not be in files
 *   2. db.properties / app.properties, e.g. db.url - used on a team member's laptop
 *
 * Both files are in .gitignore, so passwords never reach GitHub.
 *
 * Design pattern: SINGLETON-like utility - the two files are read only once.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public final class AppConfig {

    private static Properties fileSettings;

    private AppConfig() {
    }

    /**
     * The setting, or "" when it is not set anywhere.
     *
     * @param key the file key, e.g. "db.url". The environment variable has
     *            the same name in capitals with "_" for ".", e.g. DB_URL.
     */
    public static String get(String key) {
        String env = System.getenv(key.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return files().getProperty(key, "").trim();
    }

    public static boolean has(String key) {
        return !get(key).isEmpty();
    }

    private static synchronized Properties files() {
        if (fileSettings == null) {
            Properties props = new Properties();
            for (String name : new String[] {"db.properties", "app.properties"}) {
                try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(name)) {
                    if (in != null) {
                        props.load(in);
                    }
                } catch (IOException e) {
                    System.out.println("[MediSys] Could not read " + name + ": " + e.getMessage());
                }
            }
            fileSettings = props;
        }
        return fileSettings;
    }
}
