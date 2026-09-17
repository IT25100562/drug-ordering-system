/*
 * MediSys shared JavaScript - loaded on every page by header.jspf.
 * Shared file - agree with the team before changing it.
 *
 * Helpers any page can use just by adding an attribute:
 *
 *   <form data-confirm="Delete this item?">   asks before the form is sent
 *   <div data-autohide>                       fades out after a few seconds
 *   <div data-stepper> - input + </div>       quantity buttons
 *
 * and functions for the module scripts:
 *
 *   showToast(message, type, link)    small pop-up message
 *   postForm(form)                    sends a form with fetch() and returns the JSON answer
 *   showFormErrors(form, errors)      list of errors at the top of a form
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
                event.stopImmediatePropagation();
            }
        });
    });

    // Hide success messages after 5 seconds.
    document.querySelectorAll("[data-autohide]").forEach(function (box) {
        setTimeout(function () { fadeOut(box); }, 5000);
    });

    // Quantity steppers: the - and + buttons change the number next to them.
    document.querySelectorAll("[data-stepper]").forEach(function (stepper) {
        var input = stepper.querySelector("input");
        stepper.querySelectorAll("[data-step]").forEach(function (button) {
            button.addEventListener("click", function () {
                var min = Number(input.min || 1);
                var max = Number(input.max || 99);
                var value = (parseInt(input.value, 10) || min) + Number(button.getAttribute("data-step"));
                value = Math.min(Math.max(value, min), max);
                if (String(value) !== input.value) {
                    input.value = value;
                    input.dispatchEvent(new Event("change", { bubbles: true }));
                }
                updateStepperButtons(stepper);
            });
        });
        input.addEventListener("input", function () { updateStepperButtons(stepper); });
        input.addEventListener("change", function () { updateStepperButtons(stepper); });
        updateStepperButtons(stepper);
    });
});

/** Greys out - at the minimum and + at the maximum. */
function updateStepperButtons(stepper) {
    var input = stepper.querySelector("input");
    var value = parseInt(input.value, 10) || 0;
    stepper.querySelector('[data-step="-1"]').disabled = value <= Number(input.min || 1);
    stepper.querySelector('[data-step="1"]').disabled = value >= Number(input.max || 99);
}

function fadeOut(element, then) {
    element.classList.add("fading");
    setTimeout(function () {
        element.remove();
        if (then) {
            then();
        }
    }, 300);
}

/**
 * Shows a small message in the corner for a few seconds.
 *
 * @param type "success", "error" or "info"
 * @param link optional { text: "View cart", href: "/medisys/cart" }
 */
function showToast(message, type, link) {
    var area = document.querySelector(".toast-area");
    if (!area) {
        return;
    }
    var toast = document.createElement("div");
    toast.className = "toast " + (type || "info");
    toast.setAttribute("role", type === "error" ? "alert" : "status");

    var text = document.createElement("span");
    text.textContent = message;          // textContent, so no HTML is ever run
    toast.appendChild(text);

    if (link) {
        var a = document.createElement("a");
        a.href = link.href;
        a.textContent = link.text;
        toast.appendChild(a);
    }

    var close = document.createElement("button");
    close.type = "button";
    close.setAttribute("aria-label", "Close");
    close.textContent = "×";
    close.addEventListener("click", function () { fadeOut(toast); });
    toast.appendChild(close);

    area.appendChild(toast);
    // Keep at most 3 messages on screen.
    while (area.children.length > 3) {
        area.firstChild.remove();
    }
    setTimeout(function () {
        if (toast.isConnected) {
            fadeOut(toast);
        }
    }, link ? 6000 : 4000);
}

/**
 * Sends a form in the background and returns a Promise with the server's JSON.
 * When the session has ended, it goes to the login page instead.
 */
function postForm(form) {
    // getAttribute, because a field called "action" inside the form hides form.action.
    return fetch(form.getAttribute("action"), {
        method: "POST",
        headers: {
            "Accept": "application/json",
            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
        },
        body: new URLSearchParams(new FormData(form)),
        credentials: "same-origin"
    }).then(function (response) {
        if (response.status === 401) {
            var ctx = document.body.getAttribute("data-ctx") || "";
            var here = window.location.pathname.substring(ctx.length) + window.location.search;
            window.location.href = ctx + "/login?returnTo=" + encodeURIComponent(here);
            return new Promise(function () {});     // never finishes; the page is changing
        }
        var type = response.headers.get("Content-Type") || "";
        if (type.indexOf("application/json") === -1) {
            throw new Error("Unexpected answer from the server");
        }
        return response.json();
    });
}

/** Shows a list of error messages in a box at the top of a form. */
function showFormErrors(form, errors) {
    var box = form.querySelector(".js-errors");
    if (!box) {
        box = document.createElement("div");
        box.className = "message error js-errors";
        box.setAttribute("role", "alert");
        form.insertBefore(box, form.firstChild);
    }
    box.innerHTML = "";
    var list = document.createElement("ul");
    errors.forEach(function (text) {
        var item = document.createElement("li");
        item.textContent = text;
        list.appendChild(item);
    });
    box.appendChild(list);
    box.scrollIntoView({ behavior: "smooth", block: "center" });
}
