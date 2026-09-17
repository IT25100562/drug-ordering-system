package com.medisys.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A customer's whole cart: the lines plus the totals.
 *
 * Module 02 (checkout) uses this to build the order:
 *   Cart cart = cartService.getCart(userId);
 *   if (!cart.isReadyForCheckout()) { ...send the customer back to /cart... }
 *
 * Module : 01 - Shopping Cart and Wishlist
 * Owner  : Amadini G. G. A.
 */
public class Cart {

    private final List<CartItem> items;

    public Cart(List<CartItem> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** Total number of packs (sum of quantities). */
    public int getItemCount() {
        int count = 0;
        for (CartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    /** Sum of all line totals. */
    public BigDecimal getSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            total = total.add(item.getLineTotal());
        }
        return total;
    }

    /** How many lines have a problem that blocks checkout. */
    public int getProblemCount() {
        int count = 0;
        for (CartItem item : items) {
            if (item.hasProblem()) {
                count++;
            }
        }
        return count;
    }

    /** True when the cart has items and none of them has a problem. */
    public boolean isReadyForCheckout() {
        return !items.isEmpty() && getProblemCount() == 0;
    }
}
