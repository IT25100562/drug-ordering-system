package com.medisys.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Where uploaded files (prescriptions) are kept.
 *
 * Design pattern: STRATEGY. The rest of the app only talks to this interface.
 * Today the files are kept in a local folder (LocalFileStorage). Later a cloud
 * storage (Cloudinary, Supabase Storage ...) can be added as another class,
 * and StorageFactory picks one from app.properties - no other code changes.
 *
 * A file is identified by a "key" such as "prescriptions/5f1c...e2.png".
 * The key is created by the storage, never taken from the user.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public interface FileStorage {

    /**
     * Saves a new file under a fresh random name.
     *
     * @param folder      e.g. "prescriptions"
     * @param extension   e.g. "png" (decided by the caller from the file's real type)
     * @param contentType e.g. "image/png"
     * @return the key to store in the database
     */
    String save(InputStream data, String folder, String extension, String contentType) throws IOException;

    /** Saves a file under a known key (used for the demo files). */
    void put(String key, InputStream data, String contentType) throws IOException;

    boolean exists(String key) throws IOException;

    /** Opens a saved file for reading. The caller must close the stream. */
    InputStream open(String key) throws IOException;

    /** Deletes a file. Does nothing if it is already gone. */
    void delete(String key) throws IOException;

    /** Short text for the startup log, e.g. "local folder C:\Users\...". */
    String describe();
}
