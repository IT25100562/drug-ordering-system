/*
 * Medicine Catalog and Inventory - page scripts.
 *
 * Module : 03 - Medicine Catalog and Inventory
 * Owner  : Divisekara A. W. D. M. D. M. B.
 *
 * - checks the add / edit medicine form before it is sent
 *   (the same rules as MedicineService, which checks again on the server)
 * - sends the catalog / inventory filter as soon as the category changes
 */
document.addEventListener("DOMContentLoaded", function () {

    // ------------------------------------------------ add / edit form
    var form = document.getElementById("medicine-form");
    if (form) {
        form.addEventListener("submit", function (event) {
            var errors = validateMedicineForm(form);
            if (errors.length > 0) {
                event.preventDefault();
                showFormErrors(form, errors);
            }
        });

        // Live character counter for the description.
        var description = form.elements["description"];
        var counter = document.getElementById("description-count");
        if (description && counter) {
            var update = function () {
                counter.textContent = description.value.length + " / " + description.maxLength;
            };
            description.addEventListener("input", update);
            update();
        }
    }

    // ------------------------------------------- auto-submit filters
    document.querySelectorAll("select[data-autosubmit]").forEach(function (select) {
        select.addEventListener("change", function () {
            select.form.submit();
        });
    });
});

function validateMedicineForm(form) {
    var errors = [];
    var value = function (name) { return form.elements[name].value.trim(); };

    var name = value("name");
    if (name.length < 2) {
        errors.push("Name must have at least 2 characters.");
    }
    if (value("categoryId") === "") {
        errors.push("Please choose a category.");
    }
    if (value("dosageForm") === "") {
        errors.push("Please choose a dosage form.");
    }

    var price = value("price");
    if (!/^\d+(\.\d{1,2})?$/.test(price) || Number(price) <= 0) {
        errors.push("Price must be a number more than 0 with at most 2 decimal places.");
    } else if (Number(price) > 1000000) {
        errors.push("Price cannot be more than Rs. 1,000,000.");
    }

    if (!isWholeNumberInRange(value("stockQuantity"), 0, 100000)) {
        errors.push("Stock must be a whole number from 0 to 100000.");
    }
    if (!isWholeNumberInRange(value("reorderLevel"), 0, 10000)) {
        errors.push("Reorder level must be a whole number from 0 to 10000.");
    }

    // A past expiry date is only allowed when it was already saved that way.
    var expiry = form.elements["expiryDate"];
    if (expiry.value !== "" && expiry.value !== expiry.defaultValue) {
        if (expiry.value < localToday()) {
            errors.push("Expiry date cannot be in the past.");
        }
    }
    return errors;
}

/** Today's date on this computer as yyyy-mm-dd (same format as a date input). */
function localToday() {
    var now = new Date();
    var pad = function (n) { return (n < 10 ? "0" : "") + n; };
    return now.getFullYear() + "-" + pad(now.getMonth() + 1) + "-" + pad(now.getDate());
}

function isWholeNumberInRange(text, min, max) {
    if (!/^\d+$/.test(text)) {
        return false;
    }
    var number = Number(text);
    return number >= min && number <= max;
}
