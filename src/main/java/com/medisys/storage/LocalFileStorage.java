package com.medisys.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Keeps uploaded files in a folder on this computer.
 *
 * The folder is OUTSIDE the web app, so a file can never be opened by typing
 * its address - it is only sent by a servlet that checked who is asking
 * (PrescriptionFileServlet).
 *
 * Every key is checked against a strict pattern before it is turned into a
 * path, so a key like "../../windows/system.ini" can never be used.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public class LocalFileStorage implements FileStorage {

    /** folder/name.ext with only safe characters, e.g. prescriptions/ab12.png */
    private static final Pattern SAFE_KEY =
            Pattern.compile("[a-z0-9-]{1,40}/[A-Za-z0-9-]{1,64}\\.(png|jpg|pdf)");

    private final Path root;

    public LocalFileStorage(Path root) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    @Override
    public String save(InputStream data, String folder, String extension, String contentType)
            throws IOException {
        String key = folder + "/" + UUID.randomUUID() + "." + extension;
        put(key, data, contentType);
        return key;
    }

    @Override
    public void put(String key, InputStream data, String contentType) throws IOException {
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

    @Override
    public boolean exists(String key) throws IOException {
        return Files.isRegularFile(toPath(key));
    }

    @Override
    public InputStream open(String key) throws IOException {
        return Files.newInputStream(toPath(key));
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(toPath(key));
    }

    @Override
    public String describe() {
        return "local folder " + root;
    }

    /** Turns a key into a path inside the root folder, or refuses it. */
    private Path toPath(String key) throws IOException {
        if (key == null || !SAFE_KEY.matcher(key).matches()) {
            throw new IOException("Invalid file key");
        }
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IOException("Invalid file key");
        }
        return path;
    }
}
