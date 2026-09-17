package com.medisys.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Creates the one FileStorage the app uses, based on app.properties.
 *
 * Design pattern: FACTORY (and the storage object is a SINGLETON).
 *
 *   storage.type=local                  (default)
 *   storage.local.dir=C:/medisys-files  (default: <home folder>/medisys-uploads)
 *
 * app.properties is optional - without it the local defaults are used.
 * To add a cloud storage later: write e.g. CloudinaryFileStorage implements
 * FileStorage, and add a "cloudinary" case below.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public final class StorageFactory {

    private static FileStorage storage;

    private StorageFactory() {
    }

    public static synchronized FileStorage getStorage() {
        if (storage == null) {
            storage = create(loadSettings());
        }
        return storage;
    }

    private static FileStorage create(Properties settings) {
        String type = settings.getProperty("storage.type", "local").trim().toLowerCase();
        try {
            switch (type) {
                case "local":
                    String dir = settings.getProperty("storage.local.dir", "").trim();
                    Path root = dir.isEmpty()
                            ? Paths.get(System.getProperty("user.home"), "medisys-uploads")
                            : Paths.get(dir);
                    return new LocalFileStorage(root);
                default:
                    throw new IllegalStateException("Unknown storage.type in app.properties: " + type);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not prepare the file storage: " + e.getMessage(), e);
        }
    }

    private static Properties loadSettings() {
        Properties settings = new Properties();
        try (InputStream in = StorageFactory.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (in != null) {
                settings.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read app.properties", e);
        }
        return settings;
    }
}
