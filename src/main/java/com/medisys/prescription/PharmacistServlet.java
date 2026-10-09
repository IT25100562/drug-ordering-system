package com.medisys.prescription;

import com.medisys.common.FileStorage;
import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;
import com.medisys.common.ValidationException;
import com.medisys.delivery.NotificationDAO;
import com.medisys.medicine.Medicine;
import com.medisys.medicine.MedicineDAO;
import com.medisys.user.User;
import com.medisys.user.UserDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The senior pharmacist's pages.
 *
 *   GET  /pharmacist/dashboard?status=PENDING      prescriptions by status        READ
 *   GET  /pharmacist/review?id=5                   one prescription + the customer READ
 *   POST /pharmacist/review  decision=APPROVE      list the medicines, quantities and "how to use"   UPDATE
 *   POST /pharmacist/review  decision=REJECT       refuse it (a note says why)                       UPDATE
 *   POST /pharmacist/review  decision=CORRECTION   ask for a clearer copy                            UPDATE
 *   POST /pharmacist/delete                        delete an invalid / expired one                   DELETE
 *
 * Rules:
 *  - a decision can only be made while the prescription is PENDING
 *  - the price of each medicine is fixed when it is approved
 *  - reject and correction need a note for the customer
 *  - a prescription older than 30 days cannot be approved (it has expired)
 *  - a paid prescription can never be deleted
 *  - the customer is notified of every decision
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 */
@WebServlet({"/pharmacist/dashboard", "/pharmacist/review", "/pharmacist/delete"})
public class PharmacistServlet extends HttpServlet {

    private static final int NOTE_MAX = 500;
    private static final int MAX_ITEMS = 20;

    private final PrescriptionDAO prescriptionDAO = new PrescriptionDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final UserDAO userDAO = new UserDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    // ================================================================== READ

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/pharmacist/review")) {
                Integer id = TextUtil.parseInt(request.getParameter("id"));
                Prescription p = id == null ? null : prescriptionDAO.getPrescriptionById(id);
                if (p == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                showReviewPage(request, response, p, "", new String[0], new String[0], new String[0], null);
                return;
            }

            String filter = TextUtil.clean(request.getParameter("status")).toUpperCase();
            boolean known = PrescriptionStatus.fromText(filter) != null
                    || PrescriptionDAO.FILTER_PAID.equals(filter)
                    || PrescriptionDAO.FILTER_EXPIRED.equals(filter) || PrescriptionDAO.FILTER_ALL.equals(filter);
            if (!known) {
                filter = PrescriptionStatus.PENDING.name();
            }
            request.setAttribute("prescriptions", prescriptionDAO.getPrescriptions(filter));
            request.setAttribute("counts", prescriptionDAO.countForDashboard());
            request.setAttribute("filter", filter);
            request.getRequestDispatcher("/WEB-INF/views/prescription/dashboard.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Could not load the dashboard", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/pharmacist/delete")) {
                delete(request, response);
            } else {
                review(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not save the decision", e);
        }
    }

    // ================================================================ UPDATE

    /** Records the pharmacist's decision (approve / reject / correction) and tells the customer. */
    private void review(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        if (id == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String decision = TextUtil.clean(request.getParameter("decision"));
        String note = TextUtil.clean(request.getParameter("note"));
        String[] medicineIds = request.getParameterValues("medicineId");
        String[] quantities = request.getParameterValues("quantity");
        String[] dosages = request.getParameterValues("dosage");
        User pharmacist = SessionUtil.currentUser(request);

        try {
            Prescription p = prescriptionDAO.getPrescriptionById(id);
            // ---- validation
            if (!decision.equals("APPROVE") && !decision.equals("REJECT") && !decision.equals("CORRECTION")) {
                throw new ValidationException("Please choose Approve, Reject or Request correction.");
            }
            if (p == null) {
                throw new ValidationException("That prescription no longer exists.");
            }
            if (!p.isAwaitingReview()) {
                throw new ValidationException(p.getReference() + " was already handled (" + p.getStatus().getLabel() + ").");
            }
            if (note.length() > NOTE_MAX) {
                throw new ValidationException("The note can have at most " + NOTE_MAX + " characters.");
            }

            String message;
            if (decision.equals("APPROVE")) {
                if (p.isExpired()) {
                    throw new ValidationException(p.getReference() + " is older than " + Prescription.EXPIRY_DAYS
                            + " days and can no longer be approved. Reject or delete it instead.");
                }
                List<PrescriptionItem> items = readItems(medicineIds, quantities, dosages);
                if (!prescriptionDAO.approvePrescription(id, note.isEmpty() ? null : note, pharmacist.getId(), items)) {
                    throw new ValidationException(p.getReference() + " was just handled by someone else.");
                }
                BigDecimal total = BigDecimal.ZERO;
                for (PrescriptionItem item : items) {
                    total = total.add(item.getLineTotal());
                }
                String count = items.size() + " medicine" + (items.size() == 1 ? "" : "s");
                notificationDAO.addNotification(p.getUserId(), "Your prescription " + p.getReference()
                        + " was approved: " + count + ", total " + TextUtil.money(total)
                        + ". See how to use them and pay.", "/prescriptions/view?id=" + id);
                message = p.getReference() + " approved with " + count + ". The customer has been notified.";
            } else {
                boolean reject = decision.equals("REJECT");
                if (note.length() < 5) {
                    throw new ValidationException("Please write a note for the customer explaining "
                            + (reject ? "why it is rejected." : "what needs to be corrected."));
                }
                PrescriptionStatus status = reject ? PrescriptionStatus.REJECTED : PrescriptionStatus.CORRECTION_REQUESTED;
                if (!prescriptionDAO.rejectOrAskCorrection(id, status, note, pharmacist.getId())) {
                    throw new ValidationException(p.getReference() + " was just handled by someone else.");
                }
                if (reject) {
                    notificationDAO.addNotification(p.getUserId(), "Your prescription " + p.getReference()
                            + " was rejected: " + note, "/prescriptions/view?id=" + id);
                    message = p.getReference() + " rejected. The customer has been notified.";
                } else {
                    notificationDAO.addNotification(p.getUserId(), "Please upload a new copy of prescription "
                            + p.getReference() + ": " + note, "/prescriptions/correct?id=" + id);
                    message = "Correction requested for " + p.getReference() + ". The customer has been notified.";
                }
            }
            SessionUtil.flash(request, "success", message);
            response.sendRedirect(request.getContextPath() + "/pharmacist/dashboard");

        } catch (ValidationException e) {
            Prescription p = prescriptionDAO.getPrescriptionById(id);
            if (p == null) {
                SessionUtil.flash(request, "error", e.getMessage());
                response.sendRedirect(request.getContextPath() + "/pharmacist/dashboard");
                return;
            }
            // Show the page again with the messages and everything that was typed.
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showReviewPage(request, response, p, note, medicineIds, quantities, dosages, e.getErrors());
        }
    }

    /** VALIDATION of the pharmacist's medicine lines. Returns the items, or throws with every problem found. */
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
            Medicine medicine = medicineDAO.getCatalogMedicine(medicineId);
            if (medicine == null) {
                errors.add(line + "that medicine is no longer available.");
                continue;
            }
            if (!seen.add(medicine.getId())) {
                errors.add(line + medicine.getDisplayName() + " is listed twice. Put the total on one line.");
                continue;
            }
            Integer quantity = TextUtil.parseInt(qtyText);
            if (quantity == null || quantity < 1 || quantity > PrescriptionItem.QUANTITY_MAX) {
                errors.add(line + "quantity must be a whole number from 1 to " + PrescriptionItem.QUANTITY_MAX + ".");
            } else if (quantity > medicine.getStockQuantity()) {
                errors.add(line + "only " + medicine.getStockQuantity() + " of " + medicine.getDisplayName()
                        + " in stock.");
            }
            if (dosage.length() < PrescriptionItem.DOSAGE_MIN || dosage.length() > PrescriptionItem.DOSAGE_MAX) {
                errors.add(line + "please write how to use " + medicine.getDisplayName()
                        + " (" + PrescriptionItem.DOSAGE_MIN + " to " + PrescriptionItem.DOSAGE_MAX + " characters).");
            }
            if (quantity != null && quantity >= 1 && quantity <= PrescriptionItem.QUANTITY_MAX) {
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

    // ================================================================ DELETE

    /** The pharmacist deletes an unpaid prescription (invalid or expired) and its file. */
    private void delete(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Prescription p = id == null ? null : prescriptionDAO.getPrescriptionById(id);
        if (p == null) {
            SessionUtil.flash(request, "error", id == null ? "No prescription was selected."
                    : "That prescription no longer exists.");
        } else if (!p.isDeletable() || !prescriptionDAO.deletePrescription(id)) {
            SessionUtil.flash(request, "error", p.getReference() + " has been paid and must be kept.");
        } else {
            FileStorage.delete(p.getFileKey());
            notificationDAO.addNotification(p.getUserId(), "Your prescription " + p.getReference()
                    + " was removed by the pharmacy because it was "
                    + (p.isExpired() ? "older than " + Prescription.EXPIRY_DAYS + " days." : "not valid.")
                    + " You can upload a new one.", "/prescriptions/upload");
            SessionUtil.flash(request, "success", p.getReference() + " and its file were deleted. "
                    + "The customer has been notified.");
        }

        String back = request.getParameter("returnTo");
        // The review page of a deleted prescription no longer exists, so go to the dashboard.
        if (!TextUtil.isSafeLocalPath(back) || back.startsWith("/pharmacist/review")) {
            back = "/pharmacist/dashboard";
        }
        response.sendRedirect(request.getContextPath() + back);
    }

    // =============================================================== helpers

    private void showReviewPage(HttpServletRequest request, HttpServletResponse response, Prescription p,
                                String note, String[] medicineIds, String[] quantities, String[] dosages,
                                List<String> errors) throws ServletException, IOException, SQLException {
        request.setAttribute("prescription", p);
        request.setAttribute("note", note);
        request.setAttribute("rowMedicineIds", medicineIds == null ? new String[0] : medicineIds);
        request.setAttribute("rowQuantities", quantities == null ? new String[0] : quantities);
        request.setAttribute("rowDosages", dosages == null ? new String[0] : dosages);
        request.setAttribute("errors", errors);
        // Who uploaded it: photo, NIC, age, contact, red flag and history.
        request.setAttribute("customer", userDAO.getUserById(p.getUserId()));
        request.setAttribute("customerStats", userDAO.getCustomerStats(p.getUserId()));
        if (p.isAwaitingReview()) {
            // Everything on sale, by category, for the medicine drop-downs.
            List<Medicine> medicines = medicineDAO.searchMedicines(null, null, MedicineDAO.FILTER_CATALOG);
            medicines.sort(Comparator.comparing(Medicine::getCategoryName).thenComparing(Medicine::getDisplayName));
            request.setAttribute("medicines", medicines);
        }
        request.getRequestDispatcher("/WEB-INF/views/prescription/review.jsp").forward(request, response);
    }
}
