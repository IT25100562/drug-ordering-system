/*
 * MediSys shared JavaScript - loaded on every page by header.jspf.
 * Shared file - agree with the team before changing it.
 *
 * Small helpers any page can use just by adding an attribute:
 *
 *   <form data-confirm="Delete this item?">   asks before the form is sent
 *   <div data-autohide>                       fades out after a few seconds
 *
 * JavaScript checks only make the page friendlier. The Java service classes
 * always check everything again, because JavaScript can be switched off.
 */
document.addEventListener("DOMContentLoaded", function () {

    // Ask "Are you sure?" before sending forms marked with data-confirm.
    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!window.confirm(form.getAttribute("data-confirm"))) {
                event.preventDefault();
            }
        });
    });

    // Hide success messages after 5 seconds.
    document.querySelectorAll("[data-autohide]").forEach(function (box) {
        setTimeout(function () {
            box.style.transition = "opacity 0.6s";
            box.style.opacity = "0";
            setTimeout(function () { box.remove(); }, 600);
        }, 5000);
    });
});

/*
 * Shows a list of error messages in a box at the top of a form.
 * Used by the module scripts (for example medicine.js).
 */
function showFormErrors(form, errors) {
    var box = form.querySelector(".js-errors");
    if (!box) {
        box = document.createElement("div");
        box.className = "message error js-errors";
        form.insertBefore(box, form.firstChild);
    }
    box.innerHTML = "";
    var list = document.createElement("ul");
    errors.forEach(function (text) {
        var item = document.createElement("li");
        item.textContent = text;        // textContent, so no HTML is ever run
        list.appendChild(item);
    });
    box.appendChild(list);
    box.scrollIntoView({ behavior: "smooth", block: "center" });
}
