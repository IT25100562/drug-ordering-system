/*
 * Prescription Upload and Verification - page scripts.
 *
 * Module : 05 - Prescription Upload and Verification
 * Owner  : Perera D. A. A. N. S.
 *
 *  - file box: drag & drop, preview, instant type / size check
 *  - upload forms: check before sending, "Uploading..." while it is sent
 *  - review page: the medicine lines editor (add / remove, totals),
 *                 Approve only after the checklist is ticked and the lines are right,
 *                 Reject / Request correction need a note
 *  - live character counters (textarea[data-counter])
 *
 * PrescriptionService checks everything again on the server.
 */
(function () {

    var ALLOWED = {
        "image/jpeg": ["jpg", "jpeg"],
        "image/png": ["png"],
        "application/pdf": ["pdf"]
    };

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-file-drop]").forEach(setUpDropZone);
        document.querySelectorAll("form[data-upload-form]").forEach(setUpUploadForm);
        document.querySelectorAll("form[data-decision-form]").forEach(setUpDecisionForm);
        document.querySelectorAll("textarea[data-counter]").forEach(setUpCounter);
    });

    // ------------------------------------------------------ file box

    function setUpDropZone(zone) {
        var input = zone.querySelector('input[type="file"]');
        var field = zone.closest(".field");
        var error = field.querySelector("[data-file-error]");
        var preview = zone.querySelector(".drop-preview");
        var empty = zone.querySelector(".drop-empty");
        var img = preview.querySelector("img");
        var pdfIcon = preview.querySelector(".pdf-icon");
        var maxBytes = Number(zone.getAttribute("data-max-bytes"));
        var objectUrl = null;

        ["dragenter", "dragover"].forEach(function (type) {
            zone.addEventListener(type, function (event) {
                event.preventDefault();
                zone.classList.add("dragging");
            });
        });
        ["dragleave", "drop"].forEach(function (type) {
            zone.addEventListener(type, function () { zone.classList.remove("dragging"); });
        });
        zone.addEventListener("drop", function (event) {
            event.preventDefault();
            if (event.dataTransfer.files.length) {
                input.files = event.dataTransfer.files;
                input.dispatchEvent(new Event("change"));
            }
        });

        input.addEventListener("change", show);
        preview.querySelector("[data-file-clear]").addEventListener("click", function () {
            input.value = "";
            show();
            input.click();
        });

        function show() {
            if (objectUrl) {
                URL.revokeObjectURL(objectUrl);
                objectUrl = null;
            }
            var file = input.files[0];
            var problem = file ? checkFile(file, maxBytes) : null;
            setError(problem);

            if (!file || problem) {
                if (problem) {
                    input.value = "";
                }
                preview.classList.add("hidden");
                empty.classList.remove("hidden");
                zone.classList.remove("has-file");
                return;
            }
            preview.querySelector("[data-file-name]").textContent = file.name;
            preview.querySelector("[data-file-size]").textContent = formatSize(file.size);
            var isPdf = file.type === "application/pdf" || /\.pdf$/i.test(file.name);
            img.classList.toggle("hidden", isPdf);
            pdfIcon.classList.toggle("hidden", !isPdf);
            if (!isPdf) {
                objectUrl = URL.createObjectURL(file);
                img.src = objectUrl;
            }
            empty.classList.add("hidden");
            preview.classList.remove("hidden");
            zone.classList.add("has-file");
        }

        function setError(text) {
            error.textContent = text || "";
            error.classList.toggle("hidden", !text);
            zone.classList.toggle("invalid", !!text);
        }
        zone.setFileError = setError;
    }

    /** Returns a message when the file cannot be uploaded, otherwise null. */
    function checkFile(file, maxBytes) {
        var extension = (file.name.split(".").pop() || "").toLowerCase();
        var typeOk = Object.keys(ALLOWED).some(function (type) {
            return ALLOWED[type].indexOf(extension) !== -1 && (file.type === "" || file.type === type);
        });
        if (!typeOk) {
            return "\"" + file.name + "\" is not a JPG, PNG or PDF file.";
        }
        if (file.size === 0) {
            return "This file is empty.";
        }
        if (file.size > maxBytes) {
            return "This file is " + formatSize(file.size) + ". The limit is " + formatSize(maxBytes) + ".";
        }
        return null;
    }

    function formatSize(bytes) {
        if (bytes < 1024 * 1024) {
            return Math.max(1, Math.round(bytes / 1024)) + " KB";
        }
        return (bytes / (1024 * 1024)).toFixed(1).replace(".0", "") + " MB";
    }

    // --------------------------------------------------- upload forms

    function setUpUploadForm(form) {
        // Once the customer changes something, the old error list is out of date.
        form.addEventListener("change", function () {
            var box = form.querySelector(".js-errors");
            if (box) {
                box.remove();
            }
        });

        form.addEventListener("submit", function (event) {
            var errors = [];
            var file = form.elements["file"];
            if (!file.files.length) {
                errors.push("Please choose the prescription file (JPG, PNG or PDF).");
                var zone = file.closest("[data-file-drop]");
                if (zone && zone.setFileError) {
                    zone.setFileError("Please choose a file.");
                }
            }
            var confirm = form.elements["confirm"];
            if (confirm && !confirm.checked) {
                errors.push("Please confirm that the prescription was issued to you.");
            }

            if (errors.length) {
                event.preventDefault();
                showFormErrors(form, errors);
                return;
            }
            // Sending: stop double clicks and show progress.
            var button = form.querySelector('button[type="submit"]');
            button.disabled = true;
            button.classList.add("busy");
            button.textContent = button.getAttribute("data-busy-text") || "Sending...";
        });
    }

    // ----------------------------------------------------- review page

    function setUpDecisionForm(form) {
        var checks = form.querySelectorAll("[data-check]");
        var approve = form.querySelector("[data-approve]");
        var hint = form.querySelector("[data-approve-hint]");
        var note = form.elements["note"];
        var noteError = form.querySelector("[data-note-error]");
        var expired = approve.disabled;
        var rows = setUpItemRows(form);

        function updateApprove() {
            if (expired) {
                return;
            }
            var all = Array.prototype.every.call(checks, function (c) { return c.checked; });
            approve.disabled = !all;
            if (hint) {
                hint.hidden = all;
            }
        }
        checks.forEach(function (c) { c.addEventListener("change", updateApprove); });
        updateApprove();

        note.addEventListener("input", function () {
            noteError.classList.add("hidden");
            note.classList.remove("invalid");
        });

        form.addEventListener("submit", function (event) {
            var decision = event.submitter ? event.submitter.value : "";

            if (decision === "APPROVE") {
                if (rows && !rows.validate()) {
                    event.preventDefault();
                    return;
                }
                if (!window.confirm("Approve with " + rows.count() + " medicine(s), total " + rows.totalText()
                        + "? The customer will be asked to pay.")) {
                    event.preventDefault();
                    return;
                }
            } else if (note.value.trim().length < 5) {
                event.preventDefault();
                noteError.textContent = decision === "REJECT"
                        ? "Please tell the customer why the prescription is rejected."
                        : "Please tell the customer what needs to be corrected.";
                noteError.classList.remove("hidden");
                note.classList.add("invalid");
                note.focus();
                return;
            } else if (decision === "REJECT"
                    && !window.confirm("Reject this prescription? The customer will be notified.")) {
                event.preventDefault();
                return;
            }

            // Keep the chosen decision (a disabled button is not sent), then lock the buttons.
            var hidden = document.createElement("input");
            hidden.type = "hidden";
            hidden.name = "decision";
            hidden.value = decision;
            form.appendChild(hidden);
            form.querySelectorAll('button[type="submit"]').forEach(function (b) {
                b.disabled = true;
            });
            if (event.submitter) {
                event.submitter.classList.add("busy");
            }
        });
    }

    // ------------------------------------------- medicine lines editor

    /**
     * The pharmacist's medicine lines: add / remove lines, line totals and the
     * grand total, and the checks before approving.
     */
    function setUpItemRows(form) {
        var container = form.querySelector("[data-item-rows]");
        if (!container) {
            return null;
        }
        var totalOut = form.querySelector("[data-items-total]");

        function money(value) {
            return "Rs. " + value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        }

        function rowData(row) {
            var select = row.querySelector("[data-item-medicine]");
            var option = select.options[select.selectedIndex];
            return {
                select: select,
                medicineId: select.value,
                name: option ? option.textContent.split(" - ")[0].trim() : "",
                price: option && option.dataset.price ? Number(option.dataset.price) : 0,
                stock: option && option.dataset.stock ? Number(option.dataset.stock) : 0,
                qtyInput: row.querySelector("[data-item-qty]"),
                dosageInput: row.querySelector("[data-item-dosage]"),
                error: row.querySelector("[data-item-error]")
            };
        }

        function isBlank(d) {
            return d.medicineId === "" && d.dosageInput.value.trim() === "";
        }

        function recalc() {
            var total = 0;
            container.querySelectorAll("[data-item-row]").forEach(function (row) {
                var d = rowData(row);
                var qty = parseInt(d.qtyInput.value, 10) || 0;
                var line = d.medicineId ? d.price * qty : 0;
                row.querySelector("[data-item-total]").textContent = d.medicineId ? money(line) : "-";
                d.qtyInput.max = d.stock ? Math.min(d.stock, 100) : 100;
                total += line;
            });
            totalOut.textContent = money(total);
            // Always keep at least one line.
            var removeButtons = container.querySelectorAll("[data-item-remove]");
            removeButtons.forEach(function (b) { b.disabled = removeButtons.length === 1; });
        }

        function clearError(row) {
            var error = row.querySelector("[data-item-error]");
            error.classList.add("hidden");
            row.classList.remove("invalid");
        }

        container.addEventListener("input", function (e) {
            clearError(e.target.closest("[data-item-row]"));
            recalc();
        });
        container.addEventListener("change", recalc);
        container.addEventListener("click", function (e) {
            var remove = e.target.closest("[data-item-remove]");
            if (remove && container.children.length > 1) {
                remove.closest("[data-item-row]").remove();
                recalc();
            }
        });

        form.querySelector("[data-item-add]").addEventListener("click", function () {
            var template = container.querySelector("[data-item-row]");
            var row = template.cloneNode(true);
            row.querySelector("[data-item-medicine]").value = "";
            row.querySelector("[data-item-qty]").value = "1";
            row.querySelector("[data-item-dosage]").value = "";
            clearError(row);
            container.appendChild(row);
            recalc();
            row.querySelector("[data-item-medicine]").focus();
        });

        recalc();

        return {
            count: function () {
                var n = 0;
                container.querySelectorAll("[data-item-row]").forEach(function (row) {
                    if (!isBlank(rowData(row))) {
                        n++;
                    }
                });
                return n;
            },
            totalText: function () {
                return totalOut.textContent;
            },
            /** Shows a message under every wrong line. True when all lines are fine. */
            validate: function () {
                var ok = true;
                var used = {};
                var filled = 0;
                var firstBad = null;
                container.querySelectorAll("[data-item-row]").forEach(function (row) {
                    var d = rowData(row);
                    clearError(row);
                    if (isBlank(d)) {
                        return;
                    }
                    filled++;
                    var qty = Number(d.qtyInput.value);
                    var problem = null;
                    if (d.medicineId === "") {
                        problem = "Choose a medicine.";
                    } else if (used[d.medicineId]) {
                        problem = d.name + " is already on another line.";
                    } else if (!Number.isInteger(qty) || qty < 1 || qty > 100) {
                        problem = "Quantity must be 1 to 100.";
                    } else if (qty > d.stock) {
                        problem = "Only " + d.stock + " in stock.";
                    } else if (d.dosageInput.value.trim().length < 3) {
                        problem = "Write how to use " + d.name + ".";
                    }
                    used[d.medicineId] = true;
                    if (problem) {
                        ok = false;
                        d.error.textContent = problem;
                        d.error.classList.remove("hidden");
                        row.classList.add("invalid");
                        firstBad = firstBad || row;
                    }
                });
                if (filled === 0) {
                    ok = false;
                    var first = container.querySelector("[data-item-row]");
                    var error = first.querySelector("[data-item-error]");
                    error.textContent = "Add at least one medicine before approving.";
                    error.classList.remove("hidden");
                    first.classList.add("invalid");
                    firstBad = first;
                }
                if (firstBad) {
                    firstBad.scrollIntoView({ behavior: "smooth", block: "center" });
                }
                return ok;
            }
        };
    }

    // ------------------------------------------------ char counters

    function setUpCounter(textarea) {
        var out = document.getElementById(textarea.getAttribute("data-counter"));
        if (!out) {
            return;
        }
        var update = function () {
            out.textContent = textarea.value.length + " / " + textarea.maxLength;
        };
        textarea.addEventListener("input", update);
        update();
    }
})();
