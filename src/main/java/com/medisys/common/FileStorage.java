package com.medisys.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Keeps uploaded files (prescriptions and profile photos) in a folder on this
 * computer.
 *
 * The folder is OUTSIDE the web app, so a file can never be opened by typing
 * its address - it is only sent by a servlet that checked who is asking.
 * By default it is <your home folder>/medisys-uploads; app.properties
 * (storage.local.dir) can change that.
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

    private static Path root;

    private FileStorage() {
    }

    /** Saves a new file under a fresh random name and returns its key. */
    public static String save(byte[] data, String folder, String contentType) throws IOException {
        String key = folder + "/" + UUID.randomUUID() + "." + extensionFor(contentType);
        put(key, new java.io.ByteArrayInputStream(data));
        return key;
    }

    /** Saves a file under a known key (used for the demo files). */
    public static void put(String key, InputStream data) throws IOException {
        Path target = toPath(key);
        Files.createDirectories(target.getParent());
        // Write to a temporary file first, so a failed upload never leaves half a file.
        Path temp = Files.createTempFile(target.getParent(), "upload-", ".tmp");
        try {
            Files.copy(data, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static boolean exists(String key) throws IOException {
        return Files.isRegularFile(toPath(key));
    }

    /** Opens a saved file for reading. The caller must close the stream. */
    public static InputStream open(String key) throws IOException {
        return Files.newInputStream(toPath(key));
    }

    /** Deletes a file. Never fails: a problem is only written to the log. */
    public static void delete(String key) {
        try {
            Files.deleteIfExists(toPath(key));
        } catch (IOException e) {
            System.out.println("[MediSys] Could not delete file " + key + ": " + e.getMessage());
        }
    }

    /** Short text for the startup log. */
    public static String describe() throws IOException {
        return "local folder " + root();
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

    /** Turns a key into a path inside the root folder, or refuses it. */
    private static Path toPath(String key) throws IOException {
        if (key == null || !SAFE_KEY.matcher(key).matches()) {
            throw new IOException("Invalid file key");
        }
        Path folder = root();
        Path path = folder.resolve(key).normalize();
        if (!path.startsWith(folder)) {
            throw new IOException("Invalid file key");
        }
        return path;
    }

    /** The upload folder, read once from app.properties (optional). */
    private static synchronized Path root() throws IOException {
        if (root == null) {
            Properties settings = new Properties();
            try (InputStream in = FileStorage.class.getClassLoader().getResourceAsStream("app.properties")) {
                if (in != null) {
                    settings.load(in);
                }
            }
            String dir = settings.getProperty("storage.local.dir", "").trim();
            Path folder = dir.isEmpty()
                    ? Paths.get(System.getProperty("user.home"), "medisys-uploads")
                    : Paths.get(dir);
            folder = folder.toAbsolutePath().normalize();
            Files.createDirectories(folder);
            root = folder;
        }
        return root;
    }
}
