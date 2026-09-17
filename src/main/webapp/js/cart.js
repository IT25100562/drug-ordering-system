/*
 * Shopping Cart and Wishlist - page scripts.
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 *
 * Every cart / wishlist button is a normal form that works without
 * JavaScript. This script sends those forms in the background instead
 * (postForm in app.js) and updates the page in place:
 *
 *   data-ajax="cart-add"          Add to Cart (catalog, details page)
 *   data-ajax="cart-update"       quantity stepper (cart page)
 *   data-ajax="cart-remove"       Remove (cart page)
 *   data-ajax="save-for-later"    cart -> wishlist (cart page)
 *   data-ajax="wishlist-toggle"   heart button
 *   data-ajax="wishlist-move"     wishlist -> cart (wishlist page)
 *   data-ajax="wishlist-remove"   Remove (wishlist page)
 */
(function () {

    var ctx = document.body.getAttribute("data-ctx") || "";
    var cartLink = { text: "View cart", href: ctx + "/cart" };

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("form[data-ajax]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                event.preventDefault();
                handle(form, form.getAttribute("data-ajax"));
            });
        });

        // Cart page: send the new quantity shortly after the customer stops clicking.
        document.querySelectorAll('form[data-ajax="cart-update"] input[name="quantity"]').forEach(function (input) {
            var timer = null;
            input.addEventListener("change", function () {
                clearTimeout(timer);
                if (!fixQuantity(input)) {
                    return;
                }
                timer = setTimeout(function () { input.form.requestSubmit(); }, 350);
            });
        });
    });

    /** Sends the form and runs the right "after" step for its type. */
    function handle(form, type) {
        var button = form.querySelector('button[type="submit"]');
        if (form.classList.contains("busy")) {
            return;                                   // ignore double clicks
        }
        form.classList.add("busy");
        if (button) {
            button.disabled = true;
        }

        postForm(form).then(function (data) {
            updateMenuCounts(data);

            if (!data.ok && data.prescriptionRequired) {
                showToast(data.message, "info", { text: "Upload prescription", href: data.uploadUrl });
            } else if (!data.ok) {
                showToast(data.message, "error");
            }

            switch (type) {
                case "cart-add":        afterCartAdd(form, button, data); break;
                case "cart-update":     afterCartUpdate(form, data); break;
                case "cart-remove":     afterCartLineGone(form, data, null); break;
                case "save-for-later":  afterCartLineGone(form, data,
                                            { text: "View wishlist", href: ctx + "/wishlist" }); break;
                case "wishlist-toggle": afterWishlistToggle(form, data); break;
                case "wishlist-move":   afterWishlistCardGone(form, data, cartLink); break;
                case "wishlist-remove": afterWishlistCardGone(form, data, null); break;
            }
        }).catch(function () {
            showToast("Something went wrong. Please try again.", "error");
        }).finally(function () {
            form.classList.remove("busy");
            if (button) {
                button.disabled = false;
            }
        });
    }

    // ------------------------------------------------------------ add

    function afterCartAdd(form, button, data) {
        if (!data.ok) {
            return;
        }
        showToast(data.message, "success", cartLink);
        var medicineId = form.elements["medicineId"].value;
        setInCart(medicineId, data.inCart);

        // Short "Added" feedback on the button itself.
        if (button) {
            var label = button.textContent;
            button.textContent = "Added ✓";
            button.classList.add("done");
            setTimeout(function () {
                button.textContent = label;
                button.classList.remove("done");
            }, 1500);
        }

        // Details page: the quantity box may now allow fewer.
        var qty = form.elements["quantity"];
        if (qty && qty.type === "number" && form.hasAttribute("data-limit")) {
            var left = Number(form.getAttribute("data-limit")) - Number(data.inCart);
            if (left <= 0) {
                window.location.reload();             // shows the "most you can buy" notice
                return;
            }
            qty.max = left;
            qty.value = 1;
            qty.dispatchEvent(new Event("change"));
            var hint = form.querySelector(".hint");
            if (hint) {
                hint.textContent = "You can add up to " + left + " more.";
            }
        }
    }

    /** Shows "N in your cart" under the medicine. */
    function setInCart(medicineId, count) {
        document.querySelectorAll('[data-in-cart="' + medicineId + '"]').forEach(function (note) {
            note.querySelector("span").textContent = count;
            note.classList.toggle("hidden", !count);
        });
    }

    // ------------------------------------------------------ cart page

    function afterCartUpdate(form, data) {
        var line = form.closest("[data-line]");
        var input = form.elements["quantity"];

        if (!data.lineExists) {
            removeLine(line, data);
            return;
        }
        // Always show what the server really saved (undoes a refused change).
        input.value = data.quantity;
        input.setAttribute("data-last", data.quantity);
        input.dispatchEvent(new Event("input"));
        line.querySelector("[data-line-total]").textContent = data.lineTotal;
        setProblem(line, data.problem);
        updateSummary(data);
        pulse(line.querySelector("[data-line-total]"));
    }

    /**
     * Keeps a typed quantity inside 1 .. max before it is sent.
     * Returns false when nothing needs to be sent (the value went back to what it was).
     */
    function fixQuantity(input) {
        var last = Number(input.getAttribute("data-last"));
        var max = Number(input.max);
        var value = Number(input.value);

        if (input.value.trim() === "" || !Number.isInteger(value) || value < 1) {
            input.value = last;
            showToast("Quantity must be at least 1. Use Remove to take the item out.", "info");
            return false;
        }
        if (value > max) {
            input.value = max;
            showToast("You can buy at most " + max + " of this medicine.", "info");
        }
        return Number(input.value) !== last;
    }

    function afterCartLineGone(form, data, link) {
        if (!data.ok) {
            return;
        }
        showToast(data.message, "success", link);
        removeLine(form.closest("[data-line]"), data);
    }

    function removeLine(line, data) {
        fadeOut(line, function () {
            updateSummary(data);
        });
    }

    function setProblem(line, problem) {
        var box = line.querySelector("[data-problem]");
        box.textContent = problem || "";
        box.classList.toggle("hidden", !problem);
        line.classList.toggle("has-problem", !!problem);
    }

    /** Totals, item count, checkout button and the empty-cart message. */
    function updateSummary(data) {
        if (data.subtotal === undefined) {
            return;
        }
        if (data.empty) {
            var layout = document.querySelector("[data-cart]");
            if (layout) {
                layout.remove();
            }
            document.querySelector("[data-cart-empty]").classList.remove("hidden");
            setText("[data-cart-summary-text]", "Your cart is empty.");
            return;
        }
        document.querySelectorAll("[data-subtotal]").forEach(function (el) {
            el.textContent = data.subtotal;
        });
        setText("[data-item-count]", data.itemCount);
        setText("[data-cart-summary-text]",
                data.itemCount + " item" + (data.itemCount === 1 ? "" : "s") + " in your cart");

        var checkout = document.querySelector("[data-checkout]");
        checkout.classList.toggle("disabled", !data.readyForCheckout);
        if (data.readyForCheckout) {
            checkout.removeAttribute("aria-disabled");
            checkout.removeAttribute("tabindex");
        } else {
            checkout.setAttribute("aria-disabled", "true");
            checkout.setAttribute("tabindex", "-1");
        }
        var blocked = document.querySelector("[data-checkout-blocked]");
        blocked.classList.toggle("hidden", data.problemCount === 0);
        blocked.textContent = "Please fix the highlighted item" + (data.problemCount === 1 ? "" : "s")
                + " before checking out.";
    }

    // ------------------------------------------------------- wishlist

    function afterWishlistToggle(form, data) {
        if (!data.ok) {
            return;
        }
        var saved = data.saved === true;
        var heart = form.querySelector(".heart");
        heart.classList.toggle("saved", saved);
        heart.setAttribute("aria-pressed", String(saved));
        heart.title = saved ? "Remove from wishlist" : "Save to wishlist";
        pulse(heart);
        showToast(data.message, "success", saved ? { text: "View wishlist", href: ctx + "/wishlist" } : null);
    }

    function afterWishlistCardGone(form, data, link) {
        if (!data.ok) {
            return;
        }
        showToast(data.message, "success", link);
        var card = form.closest("[data-wish]");
        fadeOut(card, function () {
            var left = document.querySelectorAll("[data-wish]").length;
            setText("[data-wishlist-summary]", left === 0 ? "Nothing saved yet."
                    : left + " saved medicine" + (left === 1 ? "" : "s"));
            if (left === 0) {
                document.querySelector("[data-wishlist-empty]").classList.remove("hidden");
            }
        });
    }

    // -------------------------------------------------------- helpers

    /** The badges next to "Cart" and "Wishlist" in the menu. */
    function updateMenuCounts(data) {
        setBadge("[data-cart-count]", data.cartCount);
        setBadge("[data-wishlist-count]", data.wishlistCount);
    }

    function setBadge(selector, value) {
        if (value === undefined) {
            return;
        }
        document.querySelectorAll(selector).forEach(function (badge) {
            if (badge.textContent !== String(value)) {
                badge.textContent = value;
                pulse(badge);
            }
            badge.classList.toggle("hidden", value === 0);
        });
    }

    function setText(selector, text) {
        var el = document.querySelector(selector);
        if (el) {
            el.textContent = text;
        }
    }

    /** A short "bump" animation to show that something changed. */
    function pulse(element) {
        if (!element) {
            return;
        }
        element.classList.remove("pulse");
        void element.offsetWidth;                     // restarts the animation
        element.classList.add("pulse");
    }
})();
