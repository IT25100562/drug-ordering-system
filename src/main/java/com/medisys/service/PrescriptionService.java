package com.medisys.service;

import com.medisys.dao.PrescriptionDAO;
import com.medisys.dao.impl.PrescriptionDAOImpl;
import com.medisys.model.Medicine;
import com.medisys.model.Prescription;
import com.medisys.model.PrescriptionItem;
import com.medisys.model.PrescriptionStatus;
import com.medisys.model.User;
import com.medisys.storage.FileStorage;
import com.medisys.storage.StorageFactory;
import com.medisys.util.TextUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All business rules of the Prescription Upload and Verification module, kept
 * in one class.
 *
 * 1. Upload (customer)
 *  - only the prescription file and an optional note - the customer does not
 *    have to read the doctor's handwriting
 *  - JPG, PNG or PDF only, at most 5 MB. The type is decided from the file's
 *    first bytes, not from its name, so a renamed .exe is refused.
 *  - the file gets a new random name; the customer's file name is only shown
 *  - at most MAX_OPEN prescriptions waiting at the same time
 *
 * 2. Verification (senior pharmacist)
 *  - a decision can only be made while the prescription is PENDING
 *  - Approve: the pharmacist writes down each medicine, the quantity and how
 *    to use it. The price is fixed at that moment.
 *  - Reject and Request correction need a note for the customer
 *  - a prescription older than 30 days cannot be approved (it has expired)
 *  - the customer is notified of every decision
 *
 * 3. Payment (customer)
 *  - only an approved, unpaid, not expired prescription can be paid
 *  - stock is taken out at the same moment (all medicines or none)
 *
 * 4. Delete
 *  - a paid prescription can never be deleted
 *  - the pharmacist can delete any other one (invalid / expired);
 *    the customer can delete their own
 *  - the stored file is deleted as well
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
public class PrescriptionService {

    public static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    public static final int NOTE_MAX = 500;
    public static final int PHARMACIST_NOTE_MIN = 5;
    public static final int MAX_OPEN = 5;
    public static final int MAX_ITEMS = 20;
    public static final int ITEM_QUANTITY_MAX = 100;
    public static final int DOSAGE_MIN = 3;
    public static final int DOSAGE_MAX = 300;

    private static final String FOLDER = "prescriptions";

    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAOImpl();
    private final MedicineService medicineService = new MedicineService();
    private final NotificationService notificationService = new NotificationService();
    private final PaymentService paymentService = new PaymentService();

    // ============================================================ customer

    /**
     * Checks and saves a new prescription.
     *
     * @param fileName the name of the file on the customer's computer
     * @param data     the file content (the servlet reads at most MAX_FILE_BYTES + 1 bytes)
     * @return the new prescription's id
     */
    public int upload(User customer, String noteText, String fileName, byte[] data)
            throws SQLException, ValidationException {
        List<String> errors = new ArrayList<>();

        String note = TextUtil.clean(noteText);
        if (note.length() > NOTE_MAX) {
            errors.add("Your note can have at most " + NOTE_MAX + " characters.");
        }
        String contentType = checkFile(fileName, data, errors);
        if (errors.isEmpty() && prescriptionDAO.countOpen(customer.getId()) >= MAX_OPEN) {
            errors.add("You already have " + MAX_OPEN + " prescriptions waiting. Please wait until our "
                    + "pharmacist has checked them.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        String key = storeFile(data, contentType);
        Prescription p = new Prescription();
        p.setUserId(customer.getId());
        p.setCustomerNote(note.isEmpty() ? null : note);
        p.setFileKey(key);
        p.setOriginalFileName(cleanFileName(fileName));
        p.setContentType(contentType);
        p.setFileSize(data.length);
        try {
            return prescriptionDAO.create(p);
        } catch (SQLException e) {
            deleteFileQuietly(key);          // do not leave a file nobody points to
            throw e;
        }
    }

    /** Uploads a corrected copy for a prescription the pharmacist sent back. */
    public Prescription uploadCorrection(User customer, int id, String fileName, byte[] data)
            throws SQLException, ValidationException {
        Prescription p = getOwnPrescription(customer, id);
        if (!p.needsCorrection()) {
            throw new ValidationException("A new copy can only be sent when the pharmacist asks for one.");
        }

        List<String> errors = new ArrayList<>();
        String contentType = checkFile(fileName, data, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        String newKey = storeFile(data, contentType);
        boolean replaced;
        try {
            replaced = prescriptionDAO.replaceFile(id, newKey, cleanFileName(fileName), contentType, data.length);
        } catch (SQLException e) {
            deleteFileQuietly(newKey);
            throw e;
        }
        if (!replaced) {
            deleteFileQuietly(newKey);
            throw new ValidationException("This prescription changed in the meantime. Please refresh the page.");
        }
        deleteFileQuietly(p.getFileKey());   // the old copy is no longer needed
        return p;
    }

    public List<Prescription> getMyPrescriptions(int userId) throws SQLException {
        return prescriptionDAO.findByUser(userId);
    }

    /** The customer's own prescription, or an error (also when it belongs to someone else). */
    public Prescription getOwnPrescription(User customer, int id) throws SQLException, ValidationException {
        Prescription p = prescriptionDAO.findById(id);
        if (p == null || p.getUserId() != customer.getId()) {
            throw new ValidationException("That prescription was not found.");
        }
        return p;
    }

    /**
     * Pays for an approved prescription (test payment) and takes the medicines
     * out of stock.
     *
     * @param form delivery fields (deliveryName, deliveryAddress, deliveryPhone)
     *             and card fields (see PaymentService.checkTestCard)
     * @return the paid prescription
     */
    public Prescription pay(User customer, int id, Map<String, String> form)
            throws SQLException, ValidationException {
        Prescription p = getOwnPrescription(customer, id);
        checkPayable(p);

        List<String> errors = new ArrayList<>();
        String name = TextUtil.clean(form.get("deliveryName"));
        String address = TextUtil.clean(form.get("deliveryAddress"));
        String phone = TextUtil.clean(form.get("deliveryPhone"));
        if (name.length() < 2 || name.length() > 100) {
            errors.add("Please enter the name of the person receiving the medicines.");
        }
        if (address.length() < 5 || address.length() > 255) {
            errors.add("Please enter the full delivery address.");
        }
        if (!phone.matches("\\+?[0-9 ]{9,15}")) {
            errors.add("Please enter a valid contact number, e.g. 0771234567.");
        }
        PaymentService.CardCheck card = paymentService.checkTestCard(form, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        String reference = paymentService.newReference();
        int result = prescriptionDAO.pay(p, reference, card.last4, name, address, phone);
        if (result == PrescriptionDAO.PAY_NOT_PAYABLE) {
            throw new ValidationException(p.getReference() + " can no longer be paid. Please refresh the page.");
        }
        if (result != PrescriptionDAO.PAY_OK) {
            String medicine = "a medicine";
            for (PrescriptionItem item : p.getItems()) {
                if (item.getMedicine().getId() == result) {
                    medicine = item.getMedicine().getDisplayName();
                }
            }
            throw new ValidationException("Sorry, " + medicine + " is no longer in stock. Please contact the "
                    + "pharmacy. Your card was not charged.");
        }

        Prescription paid = prescriptionDAO.findById(id);
        notificationService.notify(customer.getId(), "Payment " + reference + " of "
                + TextUtil.money(paid.getAmountPaid()) + " for prescription " + p.getReference()
                + " was received. We are preparing your medicines for delivery.",
                "/prescriptions/view?id=" + id);
        // TODO (module 06): create the delivery for this order here.
        return paid;
    }

    // ========================================================== pharmacist

    /** @param filter a status name or a PrescriptionDAO.FILTER_ value */
    public List<Prescription> getDashboard(String filter) throws SQLException {
        return prescriptionDAO.findForDashboard(filter);
    }

    public Map<String, Integer> getDashboardCounts() throws SQLException {
        return prescriptionDAO.countForDashboard();
    }

    /** Any prescription, or null. For the pharmacist's pages. */
    public Prescription getPrescription(int id) throws SQLException {
        return prescriptionDAO.findById(id);
    }

    /** Medicines the pharmacist can put on a prescription (everything on sale), by category. */
    public List<Medicine> getMedicinesForPrescribing() throws SQLException {
        List<Medicine> medicines = medicineService.getCatalog(null, null);
        medicines.sort(Comparator.comparing(Medicine::getCategoryName).thenComparing(Medicine::getDisplayName));
        return medicines;
    }

    /**
     * Records the pharmacist's decision and tells the customer.
     *
     * @param decisionText "APPROVE", "REJECT" or "CORRECTION"
     * @param medicineIds  for APPROVE: one entry per medicine line (blank lines are ignored)
     * @param quantities   same order as medicineIds
     * @param dosages      same order as medicineIds
     * @return a message for the pharmacist
     */
    public String decide(User pharmacist, int id, String decisionText, String noteText,
                         String[] medicineIds, String[] quantities, String[] dosages)
            throws SQLException, ValidationException {
        String decision = TextUtil.clean(decisionText);
        if (!decision.equals("APPROVE") && !decision.equals("REJECT") && !decision.equals("CORRECTION")) {
            throw new ValidationException("Please choose Approve, Reject or Request correction.");
        }

        Prescription p = prescriptionDAO.findById(id);
        if (p == null) {
            throw new ValidationException("That prescription no longer exists.");
        }
        if (!p.isAwaitingReview()) {
            throw new ValidationException(p.getReference() + " was already handled ("
                    + p.getStatus().getLabel() + ").");
        }

        String note = TextUtil.clean(noteText);
        if (note.length() > NOTE_MAX) {
            throw new ValidationException("The note can have at most " + NOTE_MAX + " characters.");
        }

        if (decision.equals("APPROVE")) {
            if (p.isExpired()) {
                throw new ValidationException(p.getReference() + " is older than " + Prescription.EXPIRY_DAYS
                        + " days and can no longer be approved. Reject or delete it instead.");
            }
            List<PrescriptionItem> items = readItems(medicineIds, quantities, dosages);
            if (!prescriptionDAO.approve(id, note.isEmpty() ? null : note, pharmacist.getId(), items)) {
                throw new ValidationException(p.getReference() + " was just handled by someone else.");
            }
            notificationService.notify(p.getUserId(), "Your prescription " + p.getReference()
                    + " was approved: " + items.size() + " medicine" + (items.size() == 1 ? "" : "s")
                    + ", total " + TextUtil.money(sumOf(items)) + ". See how to use them and pay.",
                    "/prescriptions/view?id=" + id);
            return p.getReference() + " approved with " + items.size() + " medicine"
                    + (items.size() == 1 ? "" : "s") + ". The customer has been notified.";
        }

        boolean reject = decision.equals("REJECT");
        if (note.length() < PHARMACIST_NOTE_MIN) {
            throw new ValidationException("Please write a note for the customer explaining "
                    + (reject ? "why it is rejected." : "what needs to be corrected."));
        }
        PrescriptionStatus status = reject ? PrescriptionStatus.REJECTED : PrescriptionStatus.CORRECTION_REQUESTED;
        if (!prescriptionDAO.decide(id, status, note, pharmacist.getId())) {
            throw new ValidationException(p.getReference() + " was just handled by someone else.");
        }
        if (reject) {
            notificationService.notify(p.getUserId(), "Your prescription " + p.getReference()
                    + " was rejected: " + note, "/prescriptions/view?id=" + id);
            return p.getReference() + " rejected. The customer has been notified.";
        }
        notificationService.notify(p.getUserId(), "Please upload a new copy of prescription "
                + p.getReference() + ": " + note, "/prescriptions/correct?id=" + id);
        return "Correction requested for " + p.getReference() + ". The customer has been notified.";
    }

    /**
     * Deletes a prescription and its file.
     * A pharmacist may delete any unpaid one; a customer only their own.
     */
    public Prescription delete(User user, int id) throws SQLException, ValidationException {
        Prescription p = user.isPharmacist() ? prescriptionDAO.findById(id) : getOwnPrescription(user, id);
        if (p == null) {
            throw new ValidationException("That prescription no longer exists.");
        }
        if (!p.isDeletable() || !prescriptionDAO.delete(id)) {
            throw new ValidationException(p.getReference() + " has been paid and must be kept.");
        }
        deleteFileQuietly(p.getFileKey());

        if (user.isPharmacist()) {
            notificationService.notify(p.getUserId(), "Your prescription " + p.getReference()
                    + " was removed by the pharmacy because it was "
                    + (p.isExpired() ? "older than " + Prescription.EXPIRY_DAYS + " days." : "not valid.")
                    + " You can upload a new one.", "/prescriptions/upload");
        }
        return p;
    }

    // ================================================= file access (both)

    /**
     * The prescription if this user may open its file: the customer who
     * uploaded it, or a pharmacist. Otherwise null (the servlet answers 404,
     * so nobody can even tell that the prescription exists).
     */
    public Prescription getForFileAccess(User user, int id) throws SQLException {
        Prescription p = prescriptionDAO.findById(id);
        if (p == null) {
            return null;
        }
        return user.isPharmacist() || p.getUserId() == user.getId() ? p : null;
    }

    public InputStream openFile(Prescription p) throws IOException {
        return storage().open(p.getFileKey());
    }

    // ============================================================ helpers

    /** Throws when the prescription cannot be paid right now. */
    public void checkPayable(Prescription p) throws ValidationException {
        if (p.isPaid()) {
            throw new ValidationException(p.getReference() + " is already paid.");
        }
        if (p.getStatus() != PrescriptionStatus.APPROVED) {
            throw new ValidationException(p.getReference() + " can only be paid after our pharmacist approves it.");
        }
        if (p.isExpired()) {
            throw new ValidationException(p.getReference() + " has expired. Please upload a new prescription.");
        }
        if (p.getItems().isEmpty()) {
            throw new ValidationException(p.getReference() + " has no medicines to pay for.");
        }
    }

    /** Turns the pharmacist's medicine lines into items, or throws with every problem found. */
    private List<PrescriptionItem> readItems(String[] medicineIds, String[] quantities, String[] dosages)
            throws SQLException, ValidationException {
        List<String> errors = new ArrayList<>();
        List<PrescriptionItem> items = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        int rows = medicineIds == null ? 0 : medicineIds.length;

        for (int i = 0; i < rows; i++) {
            String idText = TextUtil.clean(medicineIds[i]);
            String qtyText = quantities != null && i < quantities.length ? TextUtil.clean(quantities[i]) : "";
            String dosage = dosages != null && i < dosages.length ? TextUtil.clean(dosages[i]) : "";
            if (idText.isEmpty() && dosage.isEmpty()) {
                continue;                              // an unused empty line
            }
            String line = "Line " + (i + 1) + ": ";

            Integer medicineId = TextUtil.parseInt(idText);
            if (medicineId == null) {
                errors.add(line + "please choose a medicine.");
                continue;
            }
            Medicine medicine = medicineService.getCatalogMedicine(medicineId);
            if (medicine == null) {
                errors.add(line + "that medicine is no longer available.");
                continue;
            }
            if (!seen.add(medicine.getId())) {
                errors.add(line + medicine.getDisplayName() + " is listed twice. Put the total on one line.");
                continue;
            }
            Integer quantity = TextUtil.parseInt(qtyText);
            if (quantity == null || quantity < 1 || quantity > ITEM_QUANTITY_MAX) {
                errors.add(line + "quantity must be a whole number from 1 to " + ITEM_QUANTITY_MAX + ".");
            } else if (quantity > medicine.getStockQuantity()) {
                errors.add(line + "only " + medicine.getStockQuantity() + " of " + medicine.getDisplayName()
                        + " in stock.");
            }
            if (dosage.length() < DOSAGE_MIN || dosage.length() > DOSAGE_MAX) {
                errors.add(line + "please write how to use " + medicine.getDisplayName()
                        + " (" + DOSAGE_MIN + " to " + DOSAGE_MAX + " characters).");
            }
            if (quantity != null && quantity >= 1 && quantity <= ITEM_QUANTITY_MAX) {
                items.add(new PrescriptionItem(medicine, quantity, dosage));
            }
        }

        if (errors.isEmpty() && items.isEmpty()) {
            errors.add("Add at least one medicine with its quantity and how to use it before approving.");
        }
        if (items.size() > MAX_ITEMS) {
            errors.add("A prescription can have at most " + MAX_ITEMS + " medicines.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return items;
    }

    /** Adds up the line totals. */
    private static BigDecimal sumOf(List<PrescriptionItem> items) {
        BigDecimal sum = BigDecimal.ZERO;
        for (PrescriptionItem item : items) {
            sum = sum.add(item.getLineTotal());
        }
        return sum;
    }

    /**
     * Checks an uploaded file and returns its real content type, or adds an
     * error and returns null.
     */
    private String checkFile(String fileName, byte[] data, List<String> errors) {
        if (data == null || data.length == 0 || TextUtil.isBlank(fileName)) {
            errors.add("Please choose the prescription file (JPG, PNG or PDF).");
            return null;
        }
        if (data.length > MAX_FILE_BYTES) {
            errors.add("The file is too large. The limit is 5 MB.");
            return null;
        }
        String extension = extensionOf(fileName);
        String contentType = detectType(data);
        if (contentType == null || !extensionMatches(extension, contentType)) {
            errors.add("Only JPG, PNG or PDF files can be uploaded.");
            return null;
        }
        return contentType;
    }

    /** Looks at the first bytes ("magic numbers") to find the real file type. */
    static String detectType(byte[] d) {
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

    private static boolean extensionMatches(String extension, String contentType) {
        switch (contentType) {
            case "image/jpeg":
                return extension.equals("jpg") || extension.equals("jpeg");
            case "image/png":
                return extension.equals("png");
            default:
                return extension.equals("pdf");
        }
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).trim().toLowerCase();
    }

    /** Keeps only the last part of the name (no folders) and removes odd characters. */
    static String cleanFileName(String fileName) {
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[\\p{Cntrl}\"<>|:*?]", "_").trim();
        if (name.isEmpty()) {
            name = "prescription";
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private String storeFile(byte[] data, String contentType) throws ValidationException {
        String extension = "image/png".equals(contentType) ? "png"
                : "image/jpeg".equals(contentType) ? "jpg" : "pdf";
        try {
            return storage().save(new ByteArrayInputStream(data), FOLDER, extension, contentType);
        } catch (IOException e) {
            System.out.println("[MediSys] Could not store a prescription file: " + e.getMessage());
            throw new ValidationException("Your file could not be saved. Please try again.");
        }
    }

    private void deleteFileQuietly(String key) {
        try {
            storage().delete(key);
        } catch (IOException e) {
            System.out.println("[MediSys] Could not delete file " + key + ": " + e.getMessage());
        }
    }

    private FileStorage storage() {
        return StorageFactory.getStorage();
    }
}
