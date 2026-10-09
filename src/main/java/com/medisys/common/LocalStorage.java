package com.medisys.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * STRATEGY 1 of 2: keeps uploaded files in a folder on this computer.
 * Used when no Cloudinary keys are set, so the app also works offline.
 *
 * The folder is OUTSIDE the web app, so a file can never be opened by typing
 * its address - it is only sent by a servlet that checked who is asking.
 * Default: <your home folder>/medisys-uploads (setting: storage.local.dir).
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public class LocalStorage implements StorageStrategy {

    private final Path root;

    public LocalStorage() throws IOException {
        String dir = AppConfig.get("storage.local.dir");
        Path folder = dir.isEmpty()
                ? Paths.get(System.getProperty("user.home"), "medisys-uploads")
                : Paths.get(dir);
        root = folder.toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public void put(String key, byte[] data, String contentType) throws IOException {
        Path target = toPath(key);
        Files.createDirectories(target.getParent());
        // Write to a temporary file first, so a failed upload never leaves half a file.
        Path temp = Files.createTempFile(target.getParent(), "upload-", ".tmp");
        try {
            Files.write(temp, data);
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
    public String publicUrl(String key, int size) {
        return null;        // no CDN: CatalogServlet sends the photo itself
    }

    @Override
    public String describe() {
        return "local folder " + root;
    }

    /** Turns a key into a path inside the root folder, or refuses it. */
    private Path toPath(String key) throws IOException {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IOException("Invalid file key");
        }
        return path;
    }
}
