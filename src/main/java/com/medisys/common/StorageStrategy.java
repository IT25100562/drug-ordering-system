package com.medisys.common;

import java.io.IOException;
import java.io.InputStream;

/**
 * Where uploaded files (prescriptions, profile photos) are kept.
 *
 * Design pattern: STRATEGY. FileStorage talks only to this interface, and
 * picks one of the two strategies when the app starts:
 *   - CloudinaryStorage : the files live in Cloudinary (the live system)
 *   - LocalStorage      : the files live in a folder on this computer (offline development)
 * The servlets never know which one is used, so switching needs no code change.
 *
 * Every method gets an already-checked key such as "prescriptions/5f1c...e2.png".
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public interface StorageStrategy {

    /** Saves (or replaces) the file under this key. */
    void put(String key, byte[] data, String contentType) throws IOException;

    boolean exists(String key) throws IOException;

    /** Opens a saved file for reading. The caller must close the stream. */
    InputStream open(String key) throws IOException;

    void delete(String key) throws IOException;

    /**
     * A public CDN address for a product photo, resized to about size x size
     * pixels, or null when this storage has no CDN (then a servlet sends the file).
     */
    String publicUrl(String key, int size);

    /** Short text for the startup log, e.g. "Cloudinary (cloud demo)". */
    String describe();
}
