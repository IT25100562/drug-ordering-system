package com.medisys.servlet.prescription;

import com.medisys.service.PrescriptionService;
import com.medisys.service.ValidationException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the file the customer chose in a multipart form (field "file").
 *
 * Only MAX_FILE_BYTES + 1 bytes are read, which is enough for the service to
 * see that a file is too large without holding a huge file in memory.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
final class UploadedFile {

    final String name;
    final byte[] data;

    private UploadedFile(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    static UploadedFile read(HttpServletRequest request) throws IOException, ServletException, ValidationException {
        Part part;
        try {
            part = request.getPart("file");
        } catch (IllegalStateException e) {
            // Tomcat refuses requests above the @MultipartConfig limits.
            throw new ValidationException("The file is too large. The limit is 5 MB.");
        }
        if (part == null || part.getSize() == 0) {
            return new UploadedFile("", new byte[0]);
        }
        try (InputStream in = part.getInputStream()) {
            byte[] data = in.readNBytes(PrescriptionService.MAX_FILE_BYTES + 1);
            String name = part.getSubmittedFileName();
            return new UploadedFile(name == null ? "" : name, data);
        } finally {
            part.delete();      // remove Tomcat's temporary copy
        }
    }

    /**
     * Makes sure the multipart body was read before getParameter() is used, so
     * a too-large upload gives a clear message instead of empty fields.
     */
    static void checkSize(HttpServletRequest request) throws IOException, ServletException, ValidationException {
        try {
            request.getParts();
        } catch (IllegalStateException e) {
            throw new ValidationException("The file is too large. The limit is 5 MB.");
        }
    }
}
