package com.medisys.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The one class the servlets use for uploaded files (prescriptions and
 * profile photos).
 *
 * Design patterns:
 *   - FACADE   : servlets call simple static methods (save, open, delete) and
 *                never see where the files really are.
 *   - STRATEGY : the real work is done by a StorageStrategy, chosen once when
 *                the app starts (see strategy()):
 *                  CLOUDINARY_URL is set -> CloudinaryStorage (live system)
 *                  otherwise             -> LocalStorage (a folder on this computer)
 *
 * A file is found by its "key", e.g. "prescriptions/5f1c...e2.png". The key is
 * made here, never taken from the user, and is checked against a strict
 * pattern, so a key like "../../windows/system.ini" can never be used.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public final class FileStorage {

    /** folder/name.ext with only safe characters, e.g. prescriptions/ab12.png */
    private static final Pattern SAFE_KEY =
            Pattern.compile("[a-z0-9-]{1,40}/[A-Za-z0-9-]{1,64}\\.(png|jpg|pdf)");

    private static StorageStrategy strategy;

    private FileStorage() {
    }

    /** Saves a new file under a fresh random name and returns its key. */
    public static String save(byte[] data, String folder, String contentType) throws IOException {
        String key = folder + "/" + UUID.randomUUID() + "." + extensionFor(contentType);
        strategy().put(check(key), data, contentType);
        return key;
    }

    /** Saves a file under a known key (used for the demo files). */
    public static void put(String key, InputStream data) throws IOException {
        byte[] bytes = data.readAllBytes();
        strategy().put(check(key), bytes, detectType(bytes));
    }

    public static boolean exists(String key) throws IOException {
        return strategy().exists(check(key));
    }

    /** Opens a saved file for reading. The caller must close the stream. */
    public static InputStream open(String key) throws IOException {
        return strategy().open(check(key));
    }

    /** Deletes a file. Never fails: a problem is only written to the log. */
    public static void delete(String key) {
        try {
            strategy().delete(check(key));
        } catch (IOException e) {
            System.out.println("[MediSys] Could not delete file " + key + ": " + e.getMessage());
        }
    }

    /**
     * A CDN address for a product photo (Cloudinary), or null when the photo
     * must be sent by our own servlet (local storage, or a storage problem).
     */
    public static String publicUrl(String key, int size) {
        try {
            return strategy().publicUrl(check(key), size);
        } catch (IOException e) {
            return null;
        }
    }

    /** Short text for the startup log. */
    public static String describe() throws IOException {
        return strategy().describe();
    }

    /**
     * Finds the real type of a file from its first bytes ("magic numbers"),
     * not from its name, so a renamed .exe is refused.
     *
     * @return "image/jpeg", "image/png", "application/pdf", or null for anything else
     */
    public static String detectType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "image/png";
        }
        if (d.length >= 5 && d[0] == '%' && d[1] == 'P' && d[2] == 'D' && d[3] == 'F' && d[4] == '-') {
            return "application/pdf";
        }
        return null;
    }

    private static String extensionFor(String contentType) {
        switch (contentType) {
            case "image/png":
                return "png";
            case "image/jpeg":
                return "jpg";
            default:
                return "pdf";
        }
    }

    private static String check(String key) throws IOException {
        if (key == null || !SAFE_KEY.matcher(key).matches()) {
            throw new IOException("Invalid file key");
        }
        return key;
    }

    /** Picks the storage strategy once: Cloudinary when its URL is set, else the local folder. */
    private static synchronized StorageStrategy strategy() throws IOException {
        if (strategy == null) {
            String cloudinaryUrl = AppConfig.get("cloudinary.url");
            strategy = cloudinaryUrl.startsWith("cloudinary://")
                    ? new CloudinaryStorage(cloudinaryUrl)
                    : new LocalStorage();
        }
        return strategy;
    }
}
