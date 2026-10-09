package com.medisys.medicine;

import com.medisys.common.FileStorage;
import com.medisys.common.SessionUtil;
import com.medisys.common.TextUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The admin's medicine inventory: all the CRUD of medicines.
 *
 *   GET  /admin/medicines?filter=LOW_STOCK&q=pan   the inventory list            READ
 *   GET  /admin/medicines/edit                     empty form (add)
 *   GET  /admin/medicines/edit?id=5                filled form (edit)
 *   POST /admin/medicines/edit   id=0              save a new medicine           CREATE
 *   POST /admin/medicines/edit   id=5              save the changes              UPDATE
 *        (both can also upload a product photo, or remove it with removeImage=on)
 *   POST /admin/medicines/restock                  add delivered stock           UPDATE
 *   POST /admin/medicines/discontinue              hide from the catalog         DELETE (soft)
 *   POST /admin/medicines/discontinue action=restore   put it back
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
@MultipartConfig(maxFileSize = 3L * 1024 * 1024, maxRequestSize = 4L * 1024 * 1024)
@WebServlet({"/admin/medicines", "/admin/medicines/edit", "/admin/medicines/restock", "/admin/medicines/discontinue"})
public class MedicineServlet extends HttpServlet {

    private static final String FORM_VIEW = "/WEB-INF/views/medicine/medicine-form.jsp";

    // Limits used by the validation rules below.
    private static final BigDecimal PRICE_MAX = new BigDecimal("1000000");
    private static final int STOCK_MAX = 100000;
    private static final int REORDER_MAX = 10000;
    private static final int RESTOCK_MAX = 10000;
    private static final int IMAGE_MAX_BYTES = 2 * 1024 * 1024;

    /** Names of the input fields in medicine-form.jsp. */
    private static final String[] FIELDS = {
            "name", "categoryId", "manufacturer", "dosageForm", "strength", "description",
            "price", "stockQuantity", "reorderLevel", "requiresPrescription", "expiryDate"
    };

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (request.getServletPath().equals("/admin/medicines/edit")) {
                showEditForm(request, response);
            } else {
                showInventory(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not load the inventory", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            switch (request.getServletPath()) {
                case "/admin/medicines/edit":
                    saveMedicine(request, response);
                    break;
                case "/admin/medicines/restock":
                    restock(request);
                    redirectToInventory(request, response);
                    break;
                case "/admin/medicines/discontinue":
                    discontinueOrRestore(request);
                    redirectToInventory(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (SQLException e) {
            throw new ServletException("Could not update the medicine", e);
        }
    }

    // ================================================================== READ

    private void showInventory(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        String keyword = TextUtil.clean(request.getParameter("q"));
        Integer categoryId = TextUtil.parseInt(request.getParameter("category"));
        String filter = TextUtil.clean(request.getParameter("filter"));
        List<String> allowed = Arrays.asList(MedicineDAO.FILTER_ACTIVE, MedicineDAO.FILTER_LOW_STOCK,
                MedicineDAO.FILTER_OUT_OF_STOCK, MedicineDAO.FILTER_EXPIRED, MedicineDAO.FILTER_DISCONTINUED);
        if (!allowed.contains(filter)) {
            filter = MedicineDAO.FILTER_ACTIVE;
        }

        request.setAttribute("medicines", medicineDAO.searchMedicines(keyword, categoryId, filter));
        request.setAttribute("categories", categoryDAO.getAllCategories());
        request.setAttribute("summary", medicineDAO.getSummary());
        request.setAttribute("keyword", keyword);
        request.setAttribute("selectedCategory", categoryId);
        request.setAttribute("filter", filter);
        request.getRequestDispatcher("/WEB-INF/views/medicine/inventory.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        Map<String, String> form = new HashMap<>();
        if (id == null) {
            // Defaults for a new medicine.
            id = 0;
            form.put("stockQuantity", "0");
            form.put("reorderLevel", "10");
        } else {
            Medicine medicine = medicineDAO.getMedicineById(id);
            if (medicine == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            form = toForm(medicine);
            request.setAttribute("medicine", medicine);
        }
        showForm(request, response, id, form, null);
    }

    // ======================================================= CREATE / UPDATE

    /** id = 0 adds a new medicine, any other id updates that medicine. */
    private void saveMedicine(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Integer idParam = TextUtil.parseInt(request.getParameter("id"));
        int id = idParam == null ? 0 : idParam;
        Map<String, String> form = new HashMap<>();
        for (String field : FIELDS) {
            if (request.getParameter(field) != null) {
                form.put(field, request.getParameter(field));
            }
        }

        Medicine existing = id > 0 ? medicineDAO.getMedicineById(id) : null;
        Medicine medicine = new Medicine();
        medicine.setId(id);
        List<String> errors = new ArrayList<>();
        if (id > 0 && existing == null) {
            errors.add("That medicine no longer exists.");
        } else {
            errors = validate(form, existing, medicine);
        }
        // ---- validation of the product photo (optional): JPG / PNG, at most 2 MB,
        // the real type comes from the first bytes, not from the file name.
        byte[] image = readImage(request);
        String imageType = image.length == 0 ? null : FileStorage.detectType(image);
        if (image.length > IMAGE_MAX_BYTES) {
            errors.add("The photo is too large. The limit is 2 MB.");
        } else if (image.length > 0 && !"image/jpeg".equals(imageType) && !"image/png".equals(imageType)) {
            errors.add("The photo must be a JPG or PNG image.");
        }
        if (!errors.isEmpty()) {
            // Show the same form again with the messages and what the admin typed.
            request.setAttribute("medicine", existing);
            showForm(request, response, id, form, errors);
            return;
        }

        String message;
        if (id == 0) {
            id = medicineDAO.addMedicine(medicine);                                         // CREATE
            message = "Medicine \"" + medicine.getName() + "\" was added (ID " + id + ").";
        } else {
            medicineDAO.updateMedicine(medicine);                                           // UPDATE
            message = "Medicine \"" + medicine.getName() + "\" was updated.";
        }

        // The photo goes to the file storage (Cloudinary); the table keeps only its key.
        String oldImage = existing == null ? null : existing.getImageKey();
        if (image.length > 0) {
            String key = FileStorage.save(image, "medicines", imageType);
            medicineDAO.updateImage(id, key);                                               // UPDATE
            deleteImageFile(oldImage);
        } else if (request.getParameter("removeImage") != null && oldImage != null) {
            medicineDAO.updateImage(id, null);                                              // UPDATE
            deleteImageFile(oldImage);
        }
        SessionUtil.flash(request, "success", message);
        response.sendRedirect(request.getContextPath() + "/admin/medicines");
    }

    /** The uploaded photo, or an empty array when none was chosen. */
    private byte[] readImage(HttpServletRequest request) throws IOException, ServletException {
        String type = request.getContentType();
        if (type == null || !type.startsWith("multipart/")) {
            return new byte[0];
        }
        Part part = request.getPart("image");
        if (part == null || part.getSize() == 0) {
            return new byte[0];
        }
        try (InputStream in = part.getInputStream()) {
            return in.readNBytes(IMAGE_MAX_BYTES + 1);      // one byte more shows "too large"
        }
    }

    /** Demo photos are shared with the seed data, so only uploaded ones are deleted. */
    private void deleteImageFile(String key) {
        if (key != null && !key.startsWith("medicines/med-")) {
            FileStorage.delete(key);
        }
    }

    /**
     * VALIDATION of the add / edit form. Fills in "m" and returns every problem found.
     *
     * @param existing the medicine being edited, or null when adding
     */
    private List<String> validate(Map<String, String> form, Medicine existing, Medicine m) throws SQLException {
        List<String> errors = new ArrayList<>();

        String name = TextUtil.clean(form.get("name"));
        if (name.length() < 2) {
            errors.add("Name must have at least 2 characters.");
        } else if (name.length() > 150) {
            errors.add("Name can have at most 150 characters.");
        }
        m.setName(name);

        Integer categoryId = TextUtil.parseInt(form.get("categoryId"));
        if (categoryId == null || categoryDAO.getCategoryById(categoryId) == null) {
            errors.add("Please choose a category.");
        } else {
            m.setCategoryId(categoryId);
        }

        String dosageForm = TextUtil.clean(form.get("dosageForm"));
        if (!Arrays.asList(Medicine.DOSAGE_FORMS).contains(dosageForm)) {
            errors.add("Please choose a dosage form.");
        }
        m.setDosageForm(dosageForm);

        String strength = TextUtil.clean(form.get("strength"));
        if (strength.length() > 50) {
            errors.add("Strength can have at most 50 characters.");
        }
        m.setStrength(strength.isEmpty() ? null : strength);

        String manufacturer = TextUtil.clean(form.get("manufacturer"));
        if (manufacturer.length() > 150) {
            errors.add("Manufacturer can have at most 150 characters.");
        }
        m.setManufacturer(manufacturer.isEmpty() ? null : manufacturer);

        String description = TextUtil.clean(form.get("description"));
        if (description.length() > 2000) {
            errors.add("Description can have at most 2000 characters.");
        }
        m.setDescription(description.isEmpty() ? null : description);

        // Price: more than 0, at most 2 decimal places.
        BigDecimal price = TextUtil.parseDecimal(form.get("price"));
        if (price == null) {
            errors.add("Price must be a number, e.g. 125.50.");
        } else if (price.signum() <= 0) {
            errors.add("Price must be more than 0.");
        } else if (price.compareTo(PRICE_MAX) > 0) {
            errors.add("Price cannot be more than Rs. 1,000,000.");
        } else if (price.stripTrailingZeros().scale() > 2) {
            errors.add("Price can have at most 2 decimal places.");
        }
        m.setPrice(price);

        Integer stock = TextUtil.parseInt(form.get("stockQuantity"));
        if (stock == null || stock < 0 || stock > STOCK_MAX) {
            errors.add("Stock must be a whole number from 0 to " + STOCK_MAX + ".");
        } else {
            m.setStockQuantity(stock);
        }

        Integer reorder = TextUtil.parseInt(form.get("reorderLevel"));
        if (reorder == null || reorder < 0 || reorder > REORDER_MAX) {
            errors.add("Reorder level must be a whole number from 0 to " + REORDER_MAX + ".");
        } else {
            m.setReorderLevel(reorder);
        }

        // A checkbox is only sent when it is ticked.
        m.setRequiresPrescription(form.get("requiresPrescription") != null);

        // Expiry date: optional, but not in the past
        // (editing an already expired medicine without changing the date is allowed).
        String expiryText = TextUtil.clean(form.get("expiryDate"));
        if (!expiryText.isEmpty()) {
            LocalDate expiry = TextUtil.parseDate(expiryText);
            boolean unchanged = existing != null && expiry != null && expiry.equals(existing.getExpiryDate());
            if (expiry == null) {
                errors.add("Expiry date is not a valid date.");
            } else if (expiry.isBefore(LocalDate.now()) && !unchanged) {
                errors.add("Expiry date cannot be in the past.");
            }
            m.setExpiryDate(expiry);
        }

        // No two medicines with the same name and strength.
        if (errors.isEmpty() && medicineDAO.medicineExists(name, m.getStrength(), m.getId())) {
            errors.add("A medicine called \"" + m.getDisplayName() + "\" already exists.");
        }
        return errors;
    }

    // ================================================================ UPDATE

    /** Adds delivered stock to a medicine. */
    private void restock(HttpServletRequest request) throws SQLException {
        Medicine medicine = findMedicine(request);
        Integer quantity = TextUtil.parseInt(request.getParameter("quantity"));
        String error = null;
        if (quantity == null || quantity < 1 || quantity > RESTOCK_MAX) {
            error = "Quantity to add must be a whole number from 1 to " + RESTOCK_MAX + ".";
        } else if (medicine == null) {
            error = "That medicine no longer exists.";
        } else if (medicine.isDiscontinued()) {
            error = "Restore " + medicine.getDisplayName() + " before adding stock.";
        } else if ((long) medicine.getStockQuantity() + quantity > STOCK_MAX) {
            error = "Stock cannot go above " + STOCK_MAX + ".";
        }
        if (error != null) {
            SessionUtil.flash(request, "error", error);
            return;
        }
        medicineDAO.addStock(medicine.getId(), quantity);
        SessionUtil.flash(request, "success", "Added " + quantity + " to the stock of " + medicine.getDisplayName() + ".");
    }

    // ================================================================ DELETE

    /** Discontinue (soft delete) or, with action=restore, put back in the catalog. */
    private void discontinueOrRestore(HttpServletRequest request) throws SQLException {
        Medicine medicine = findMedicine(request);
        boolean restore = "restore".equals(request.getParameter("action"));
        if (medicine == null) {
            SessionUtil.flash(request, "error", "That medicine no longer exists.");
        } else if (restore && !medicine.isDiscontinued()) {
            SessionUtil.flash(request, "error", medicine.getDisplayName() + " is not discontinued.");
        } else if (!restore && medicine.isDiscontinued()) {
            SessionUtil.flash(request, "error", medicine.getDisplayName() + " is already discontinued.");
        } else if (restore) {
            medicineDAO.restoreMedicine(medicine.getId());
            SessionUtil.flash(request, "success", medicine.getDisplayName() + " is back in the catalog.");
        } else {
            medicineDAO.discontinueMedicine(medicine.getId());
            SessionUtil.flash(request, "success", medicine.getDisplayName()
                    + " was discontinued and hidden from the catalog.");
        }
    }

    // =============================================================== helpers

    private Medicine findMedicine(HttpServletRequest request) throws SQLException {
        Integer id = TextUtil.parseInt(request.getParameter("id"));
        return id == null ? null : medicineDAO.getMedicineById(id);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, int id,
                          Map<String, String> form, List<String> errors)
            throws SQLException, ServletException, IOException {
        request.setAttribute("id", id);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("categories", categoryDAO.getAllCategories());
        request.getRequestDispatcher(FORM_VIEW).forward(request, response);
    }

    /** Turns a saved medicine into form values. */
    private Map<String, String> toForm(Medicine m) {
        Map<String, String> form = new HashMap<>();
        form.put("name", m.getName());
        form.put("categoryId", String.valueOf(m.getCategoryId()));
        form.put("manufacturer", m.getManufacturer());
        form.put("dosageForm", m.getDosageForm());
        form.put("strength", m.getStrength());
        form.put("description", m.getDescription());
        form.put("price", m.getPrice().toPlainString());
        form.put("stockQuantity", String.valueOf(m.getStockQuantity()));
        form.put("reorderLevel", String.valueOf(m.getReorderLevel()));
        if (m.isRequiresPrescription()) {
            form.put("requiresPrescription", "on");
        }
        if (m.getExpiryDate() != null) {
            form.put("expiryDate", m.getExpiryDate().toString());
        }
        return form;
    }

    /**
     * After an action on the inventory page, go back to the same list (same
     * search and filter). The query comes from a hidden form field, so only
     * plain query characters are accepted.
     */
    private void redirectToInventory(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String query = TextUtil.clean(request.getParameter("returnQuery"));
        String url = request.getContextPath() + "/admin/medicines";
        if (query.matches("[A-Za-z0-9_=&%+.\\-]{1,300}")) {
            url += "?" + query;
        }
        response.sendRedirect(url);
    }
}
