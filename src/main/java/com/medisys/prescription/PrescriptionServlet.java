package com.medisys.prescription;

import com.medisys.common.FileStorage;
import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.ValidationException;
import com.medisys.user.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The customer's prescriptions.
 *
 *   GET  /prescriptions                 my prescriptions                        READ
 *   GET  /prescriptions/upload          the upload form
 *   POST /prescriptions/upload          save a new prescription                 CREATE
 *   GET  /prescriptions/view?id=5       one prescription (medicines, how to use, pay)  READ
 *   GET  /prescriptions/correct?id=5    form for a clearer copy
 *   POST /prescriptions/correct         send the clearer copy                   UPDATE
 *   POST /prescriptions/delete          delete my unpaid prescription           DELETE
 *   GET  /prescriptions/file?id=5       the uploaded file (customer or pharmacist)
 *
 * Upload rules:
 *  - only the file and an optional note: the customer does not have to read
 *    the doctor's handwriting, the pharmacist lists the medicines
 *  - JPG, PNG or PDF only, at most 5 MB. The real type is found from the
 *    file's first bytes, not from its name, so a renamed .exe is refused
 *  - the file gets a new random name; the customer's file name is only shown
 *  - at most 5 prescriptions waiting at the same time
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet({"/prescriptions", "/prescriptions/upload", "/prescriptions/view", "/prescriptions/correct",
        "/prescriptions/delete", "/prescriptions/file"})
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 10L * 1024 * 1024, maxRequestSize = 11L * 1024 * 1024)
public class PrescriptionServlet extends HttpServlet {

    public static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final int NOTE_MAX = 500;
    private static final int MAX_OPEN = 5;

    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            switch (request.getServletPath()) {
                case "/prescriptions":
                    request.setAttribute("prescriptions", prescriptionDAO.getPrescriptionsByUser(user.getId()));
                    request.setAttribute("highlight", TextUtil.parseInt(request.getParameter("highlight")));
                    request.getRequestDispatcher("/WEB-INF/views/prescription/my-prescriptions.jsp").forward(request, response);
                    break;
                case "/prescriptions/upload":
                    showUploadForm(request, response, "", null);
                    break;
                case "/prescriptions/view":
                    Prescription p = findOwn(request, user);
                    if (p == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    request.setAttribute("prescription", p);
                    request.getRequestDispatcher("/WEB-INF/views/prescription/view.jsp").forward(request, response);
                    break;
                case "/prescriptions/correct":
                    showCorrectionForm(request, response, user);
                    break;
                default:
                    sendFile(request, response, user);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not load the prescription", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = SessionUtil.currentUser(request);
        try {
            switch (request.getServletPath()) {
                case "/prescriptions/upload":
                    upload(request, response, user);
                    break;
                case "/prescriptions/correct":
                    uploadCorrection(request, response, user);
                    break;
                case "/prescriptions/delete":
                    delete(request, user);
                    response.sendRedirect(request.getContextPath() + "/prescriptions");
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not save the prescription", e);
        }
    }

    // ================================================================ CREATE

    private void upload(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        String note = "";
        try {
            checkRequestSize(request);
            note = TextUtil.clean(request.getParameter("note"));
            Part part = request.getPart("file");
            byte[] data = readFile(part);
            String fileName = part == null || part.getSubmittedFileName() == null ? "" : part.getSubmittedFileName();
            if (!"yes".equals(request.getParameter("confirm"))) {
                throw new ValidationException("Please confirm that the prescription was issued to you.");
            }

            // ---- validation
            List<String> errors = new ArrayList<>();
            if (note.length() > NOTE_MAX) {
                errors.add("Your note can have at most " + NOTE_MAX + " characters.");
            }
            String contentType = checkFile(fileName, data, errors);
            if (errors.isEmpty() && prescriptionDAO.countOpen(user.getId()) >= MAX_OPEN) {
                errors.add("You already have " + MAX_OPEN + " prescriptions waiting. Please wait until our "
                        + "pharmacist has checked them.");
            }
            if (!errors.isEmpty()) {
                throw new ValidationException(errors);
            }

            // ---- save the file, then the row
            String key = storeFile(data, contentType);
            Prescription p = new Prescription();
            p.setUserId(user.getId());
            p.setCustomerNote(note.isEmpty() ? null : note);
            p.setFileKey(key);
            p.setOriginalFileName(cleanFileName(fileName));
            p.setContentType(contentType);
            p.setFileSize(data.length);
            int id;
            try {
                id = prescriptionDAO.addPrescription(p);
            } catch (SQLException e) {
                FileStorage.delete(key);          // do not leave a file nobody points to
                throw e;
            }

            SessionUtil.flash(request, "success", "Prescription " + String.format("RX-%06d", id)
                    + " was uploaded. Our pharmacist will check it, list your medicines and notify you.");
            response.sendRedirect(request.getContextPath() + "/prescriptions?highlight=" + id);
        } catch (ValidationException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showUploadForm(request, response, note, e.getErrors());
        }
    }

    // ================================================================ UPDATE

    private void showCorrectionForm(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        Prescription p = findOwn(request, user);
        if (p == null) {
            SessionUtil.flash(request, "error", "That prescription was not found.");
            response.sendRedirect(request.getContextPath() + "/prescriptions");
            return;
        }
        if (!p.needsCorrection()) {
            SessionUtil.flash(request, "info", p.getReference() + " does not need a new copy ("
                    + p.getStatus().getLabel() + ").");
            response.sendRedirect(request.getContextPath() + "/prescriptions?highlight=" + p.getId());
            return;
        }
        showCorrectionForm(request, response, p, null);
    }

    /** Uploads a clearer copy for a prescription the pharmacist sent back. */
    private void uploadCorrection(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        Prescription p;
        try {
            checkRequestSize(request);
            p = findOwn(request, user);
            if (p == null) {
                throw new ValidationException("That prescription was not found.");
            }
            if (!p.needsCorrection()) {
                throw new ValidationException(p.getReference() + " does not need a new copy ("
                        + p.getStatus().getLabel() + ").");
            }
        } catch (ValidationException e) {
            SessionUtil.flash(request, "error", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/prescriptions");
            return;
        }

        try {
            Part part = request.getPart("file");
            byte[] data = readFile(part);
            String fileName = part == null || part.getSubmittedFileName() == null ? "" : part.getSubmittedFileName();

            // ---- validation
            List<String> errors = new ArrayList<>();
            String contentType = checkFile(fileName, data, errors);
            if (!errors.isEmpty()) {
                throw new ValidationException(errors);
            }

            String newKey = storeFile(data, contentType);
            boolean replaced;
            try {
                replaced = prescriptionDAO.replaceFile(p.getId(), newKey, cleanFileName(fileName), contentType, data.length);
            } catch (SQLException e) {
                FileStorage.delete(newKey);
                throw e;
            }
            if (!replaced) {
                FileStorage.delete(newKey);
                throw new ValidationException("This prescription changed in the meantime. Please refresh the page.");
            }
            FileStorage.delete(p.getFileKey());   // the old copy is no longer needed

            SessionUtil.flash(request, "success", "Thank you. The new copy of " + p.getReference()
                    + " was sent to our pharmacist.");
            response.sendRedirect(request.getContextPath() + "/prescriptions?highlight=" + p.getId());
        } catch (ValidationException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showCorrectionForm(request, response, p, e.getErrors());
        }
    }

    // ================================================================ DELETE

    /** The customer deletes their own prescription (never a paid one). */
    private void delete(HttpServletRequest request, User user) throws SQLException {
        Prescription p = findOwn(request, user);
        if (p == null) {
            SessionUtil.flash(request, "error", "That prescription was not found.");
        } else if (!p.isDeletable() || !prescriptionDAO.deletePrescription(p.getId())) {
            SessionUtil.flash(request, "error", p.getReference() + " has been paid and must be kept.");
        } else {
            FileStorage.delete(p.getFileKey());
            SessionUtil.flash(request, "success", "Prescription " + p.getReference() + " was deleted.");
        }
    }

    // ================================================================== READ

    /**
     * Sends the uploaded file: only to the customer who uploaded it, or a
     * pharmacist. Anyone else gets 404, so nobody can tell that it exists.
     */
    private void sendFile(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Prescription p = id == null ? null : prescriptionDAO.getPrescriptionById(id);
        if (p == null || (!user.isPharmacist() && p.getUserId() != user.getId())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try (InputStream in = FileStorage.open(p.getFileKey())) {
            boolean download = "1".equals(request.getParameter("download"));
            String fileName = URLEncoder.encode(p.getOriginalFileName(), StandardCharsets.UTF_8).replace("+", "%20");
            // The type comes from our database (checked at upload), never from the file name.
            response.setContentType(p.getContentType());
            response.setContentLength(p.getFileSize());
            response.setHeader("Content-Disposition",
                    (download ? "attachment" : "inline") + "; filename*=UTF-8''" + fileName);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "private, no-store");
            in.transferTo(response.getOutputStream());
        } catch (NoSuchFileException e) {
            log("Prescription file missing: " + p.getFileKey());
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // =============================================================== helpers

    /** The customer's own prescription from the "id" parameter, or null (also for someone else's). */
    private Prescription findOwn(HttpServletRequest request, User user) throws SQLException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Prescription p = id == null ? null : prescriptionDAO.getPrescriptionById(id);
        return p == null || p.getUserId() != user.getId() ? null : p;
    }

    /**
     * Reads the whole multipart body before getParameter() is used, so a
     * too-large upload gives a clear message instead of empty fields.
     */
    private static void checkRequestSize(HttpServletRequest request)
            throws IOException, ServletException, ValidationException {
        try {
            request.getParts();
        } catch (IllegalStateException e) {
            // Tomcat refuses requests above the @MultipartConfig limits.
            throw new ValidationException("The file is too large. The limit is 5 MB.");
        }
    }

    /** Reads at most MAX_FILE_BYTES + 1 bytes: enough to notice a file that is too big. */
    private static byte[] readFile(Part part) throws IOException {
        if (part == null || part.getSize() == 0) {
            return new byte[0];
        }
        try (InputStream in = part.getInputStream()) {
            return in.readNBytes(MAX_FILE_BYTES + 1);
        } finally {
            part.delete();      // remove Tomcat's temporary copy
        }
    }

    /** VALIDATION of an uploaded file. Returns its real content type, or adds an error and returns null. */
    private static String checkFile(String fileName, byte[] data, List<String> errors) {
        if (data.length == 0 || TextUtil.isBlank(fileName)) {
            errors.add("Please choose the prescription file (JPG, PNG or PDF).");
            return null;
        }
        if (data.length > MAX_FILE_BYTES) {
            errors.add("The file is too large. The limit is 5 MB.");
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        String extension = dot < 0 ? "" : fileName.substring(dot + 1).trim().toLowerCase();
        String contentType = FileStorage.detectType(data);
        boolean extensionMatches = contentType != null && (contentType.equals("image/jpeg")
                ? extension.equals("jpg") || extension.equals("jpeg")
                : contentType.equals("image/png") ? extension.equals("png") : extension.equals("pdf"));
        if (!extensionMatches) {
            errors.add("Only JPG, PNG or PDF files can be uploaded.");
            return null;
        }
        return contentType;
    }

    /** Keeps only the last part of the name (no folders) and removes odd characters. */
    private static String cleanFileName(String fileName) {
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}\"<>|:*?]", "_").trim();
        if (name.isEmpty()) {
            name = "prescription";
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private static String storeFile(byte[] data, String contentType) throws ValidationException {
        try {
            return FileStorage.save(data, "prescriptions", contentType);
        } catch (IOException e) {
            System.out.println("[MediSys] Could not store a prescription file: " + e.getMessage());
            throw new ValidationException("Your file could not be saved. Please try again.");
        }
    }

    private void showUploadForm(HttpServletRequest request, HttpServletResponse response,
                                String note, List<String> errors) throws ServletException, IOException {
        request.setAttribute("note", note);
        request.setAttribute("errors", errors);
        request.setAttribute("maxFileBytes", MAX_FILE_BYTES);
        request.getRequestDispatcher("/WEB-INF/views/prescription/upload.jsp").forward(request, response);
    }

    private void showCorrectionForm(HttpServletRequest request, HttpServletResponse response,
                                    Prescription p, List<String> errors) throws ServletException, IOException {
        request.setAttribute("prescription", p);
        request.setAttribute("errors", errors);
        request.setAttribute("maxFileBytes", MAX_FILE_BYTES);
        request.getRequestDispatcher("/WEB-INF/views/prescription/correct.jsp").forward(request, response);
    }
}
