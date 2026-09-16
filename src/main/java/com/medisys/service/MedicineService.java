package com.medisys.service;

import com.medisys.dao.CategoryDAO;
import com.medisys.dao.MedicineDAO;
import com.medisys.dao.impl.CategoryDAOImpl;
import com.medisys.dao.impl.MedicineDAOImpl;
import com.medisys.model.Category;
import com.medisys.model.InventorySummary;
import com.medisys.model.Medicine;
import com.medisys.util.TextUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * All business rules of the Medicine Catalog and Inventory module.
 *
 * Servlets only read the request and call this class. This class checks the
 * rules and then calls the DAOs. Other modules use it too:
 *   - 01 (cart)   : getAvailableMedicine()
 *   - 02 (orders) : reduceStock() when an order is placed
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 */
public class MedicineService {

    // Limits used by the validation rules below.
    public static final int NAME_MAX = 150;
    public static final int MANUFACTURER_MAX = 150;
    public static final int STRENGTH_MAX = 50;
    public static final int DESCRIPTION_MAX = 2000;
    public static final BigDecimal PRICE_MAX = new BigDecimal("1000000");
    public static final int STOCK_MAX = 100000;
    public static final int REORDER_MAX = 10000;
    public static final int RESTOCK_MAX = 10000;
    public static final int CATEGORY_NAME_MAX = 100;
    public static final int CATEGORY_DESCRIPTION_MAX = 255;

    private final MedicineDAO medicineDAO = new MedicineDAOImpl();
    private final CategoryDAO categoryDAO = new CategoryDAOImpl();

    // ============================================================ catalog

    /** Medicines customers can browse: not discontinued and not expired. */
    public List<Medicine> getCatalog(String keyword, Integer categoryId) throws SQLException {
        return medicineDAO.search(keyword, categoryId, MedicineDAO.FILTER_CATALOG);
    }

    /**
     * A medicine a customer is allowed to see. Returns null for unknown,
     * discontinued or expired medicines.
     */
    public Medicine getCatalogMedicine(int id) throws SQLException {
        Medicine medicine = medicineDAO.findById(id);
        if (medicine == null || medicine.isDiscontinued() || medicine.isExpired()) {
            return null;
        }
        return medicine;
    }

    /**
     * For module 01 (cart): a medicine that can be bought right now.
     * Throws a ValidationException with a message for the customer otherwise.
     */
    public Medicine getAvailableMedicine(int id) throws SQLException, ValidationException {
        Medicine medicine = getCatalogMedicine(id);
        if (medicine == null) {
            throw new ValidationException("This medicine is not available.");
        }
        if (medicine.isOutOfStock()) {
            throw new ValidationException(medicine.getDisplayName() + " is out of stock.");
        }
        return medicine;
    }

    // ========================================================== inventory

    /** Medicines for the admin inventory page. filter is a MedicineDAO.FILTER_ value. */
    public List<Medicine> getInventory(String keyword, Integer categoryId, String filter) throws SQLException {
        List<String> allowed = Arrays.asList(MedicineDAO.FILTER_ACTIVE, MedicineDAO.FILTER_LOW_STOCK,
                MedicineDAO.FILTER_OUT_OF_STOCK, MedicineDAO.FILTER_EXPIRED, MedicineDAO.FILTER_DISCONTINUED);
        if (!allowed.contains(filter)) {
            filter = MedicineDAO.FILTER_ACTIVE;
        }
        return medicineDAO.search(keyword, categoryId, filter);
    }

    public InventorySummary getSummary() throws SQLException {
        return medicineDAO.getSummary();
    }

    /** Any medicine (also discontinued / expired), or null. For admin pages. */
    public Medicine getMedicine(int id) throws SQLException {
        return medicineDAO.findById(id);
    }

    /**
     * Checks the add / edit form and saves it.
     *
     * @param id   0 to add a new medicine, otherwise the id to update
     * @param form the form fields by name (see medicine-form.jsp)
     * @return the id of the saved medicine
     */
    public int saveMedicine(int id, Map<String, String> form) throws SQLException, ValidationException {
        Medicine existing = null;
        if (id > 0) {
            existing = medicineDAO.findById(id);
            if (existing == null) {
                throw new ValidationException("That medicine no longer exists.");
            }
        }

        List<String> errors = new ArrayList<>();
        Medicine m = new Medicine();
        m.setId(id);

        // --- name
        String name = TextUtil.clean(form.get("name"));
        if (name.length() < 2) {
            errors.add("Name must have at least 2 characters.");
        } else if (name.length() > NAME_MAX) {
            errors.add("Name can have at most " + NAME_MAX + " characters.");
        }
        m.setName(name);

        // --- category must exist
        Integer categoryId = TextUtil.parseInt(form.get("categoryId"));
        if (categoryId == null || categoryDAO.findById(categoryId) == null) {
            errors.add("Please choose a category.");
        } else {
            m.setCategoryId(categoryId);
        }

        // --- dosage form must be one from the list
        String dosageForm = TextUtil.clean(form.get("dosageForm"));
        if (!Arrays.asList(Medicine.DOSAGE_FORMS).contains(dosageForm)) {
            errors.add("Please choose a dosage form.");
        }
        m.setDosageForm(dosageForm);

        // --- optional text fields
        String strength = TextUtil.clean(form.get("strength"));
        if (strength.length() > STRENGTH_MAX) {
            errors.add("Strength can have at most " + STRENGTH_MAX + " characters.");
        }
        m.setStrength(strength.isEmpty() ? null : strength);

        String manufacturer = TextUtil.clean(form.get("manufacturer"));
        if (manufacturer.length() > MANUFACTURER_MAX) {
            errors.add("Manufacturer can have at most " + MANUFACTURER_MAX + " characters.");
        }
        m.setManufacturer(manufacturer.isEmpty() ? null : manufacturer);

        String description = TextUtil.clean(form.get("description"));
        if (description.length() > DESCRIPTION_MAX) {
            errors.add("Description can have at most " + DESCRIPTION_MAX + " characters.");
        }
        m.setDescription(description.isEmpty() ? null : description);

        // --- price: more than 0, at most 2 decimal places
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

        // --- stock and reorder level: whole numbers in range
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

        // --- checkbox: present only when ticked
        m.setRequiresPrescription(form.get("requiresPrescription") != null);

        // --- expiry date: optional, but not in the past.
        //     (Editing an already expired medicine without changing the date is allowed.)
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

        // --- no two medicines with the same name and strength
        if (errors.isEmpty() && medicineDAO.existsByNameAndStrength(name, m.getStrength(), id)) {
            errors.add("A medicine called \"" + m.getDisplayName() + "\" already exists.");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        if (id == 0) {
            return medicineDAO.create(m);
        }
        medicineDAO.update(m);
        return id;
    }

    /** Adds delivered stock to a medicine. */
    public Medicine restock(int id, String quantityText) throws SQLException, ValidationException {
        Integer quantity = TextUtil.parseInt(quantityText);
        if (quantity == null || quantity < 1 || quantity > RESTOCK_MAX) {
            throw new ValidationException("Quantity to add must be a whole number from 1 to " + RESTOCK_MAX + ".");
        }
        Medicine medicine = requireMedicine(id);
        if (medicine.isDiscontinued()) {
            throw new ValidationException("Restore " + medicine.getDisplayName() + " before adding stock.");
        }
        if ((long) medicine.getStockQuantity() + quantity > STOCK_MAX) {
            throw new ValidationException("Stock cannot go above " + STOCK_MAX + ".");
        }
        medicineDAO.addStock(id, quantity);
        return medicine;
    }

    /**
     * For module 02 (orders): takes stock out when an order is placed.
     * Fails, without changing anything, when there is not enough stock.
     */
    public void reduceStock(int id, int quantity) throws SQLException, ValidationException {
        if (quantity < 1) {
            throw new ValidationException("Quantity must be at least 1.");
        }
        Medicine medicine = requireMedicine(id);
        if (!medicineDAO.reduceStock(id, quantity)) {
            throw new ValidationException("Only " + medicine.getStockQuantity() + " of "
                    + medicine.getDisplayName() + " left in stock.");
        }
    }

    /** Hides a medicine from the catalog (soft delete). */
    public Medicine discontinue(int id) throws SQLException, ValidationException {
        Medicine medicine = requireMedicine(id);
        if (medicine.isDiscontinued()) {
            throw new ValidationException(medicine.getDisplayName() + " is already discontinued.");
        }
        medicineDAO.setDiscontinued(id, true);
        return medicine;
    }

    /** Puts a discontinued medicine back into the catalog. */
    public Medicine restore(int id) throws SQLException, ValidationException {
        Medicine medicine = requireMedicine(id);
        if (!medicine.isDiscontinued()) {
            throw new ValidationException(medicine.getDisplayName() + " is not discontinued.");
        }
        medicineDAO.setDiscontinued(id, false);
        return medicine;
    }

    // ========================================================= categories

    public List<Category> getCategories() throws SQLException {
        return categoryDAO.findAll();
    }

    public void addCategory(String nameText, String descriptionText) throws SQLException, ValidationException {
        String name = TextUtil.clean(nameText);
        String description = TextUtil.clean(descriptionText);

        List<String> errors = new ArrayList<>();
        if (name.length() < 2 || name.length() > CATEGORY_NAME_MAX) {
            errors.add("Category name must have 2 to " + CATEGORY_NAME_MAX + " characters.");
        } else if (categoryDAO.existsByName(name)) {
            errors.add("A category called \"" + name + "\" already exists.");
        }
        if (description.length() > CATEGORY_DESCRIPTION_MAX) {
            errors.add("Description can have at most " + CATEGORY_DESCRIPTION_MAX + " characters.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        categoryDAO.create(new Category(0, name, description.isEmpty() ? null : description));
    }

    /** Deletes a category, but only when no medicine uses it. */
    public Category deleteCategory(int id) throws SQLException, ValidationException {
        Category category = categoryDAO.findById(id);
        if (category == null) {
            throw new ValidationException("That category no longer exists.");
        }
        int used = categoryDAO.countMedicines(id);
        if (used > 0) {
            throw new ValidationException("\"" + category.getName() + "\" is used by " + used
                    + " medicine(s). Move them to another category first.");
        }
        categoryDAO.delete(id);
        return category;
    }

    // ============================================================ helpers

    private Medicine requireMedicine(int id) throws SQLException, ValidationException {
        Medicine medicine = medicineDAO.findById(id);
        if (medicine == null) {
            throw new ValidationException("That medicine no longer exists.");
        }
        return medicine;
    }
}
