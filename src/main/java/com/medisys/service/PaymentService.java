package com.medisys.service;

import com.medisys.util.TextUtil;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TEST payment gateway. No real money moves: any card in a valid shape is
 * accepted. The full card number and the CVV are never stored or logged -
 * only the last 4 digits are kept for the receipt.
 *
 * PLACEHOLDER: module 02 (Order Placement and Checkout) owns payments. Module
 * 05 uses checkTestCard() for paying an approved prescription. Module 02 can
 * reuse it for the cart checkout, or replace it with a real gateway.
 *
 * Module : 02 - Order Placement and Checkout
 * Owner  : Hewage B. H. A. S.
 *
 * TODO (module 02): orders, order items and the cart checkout.
 */
public class PaymentService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** The result of a successful card check. */
    public static class CardCheck {
        public final String last4;

        CardCheck(String last4) {
            this.last4 = last4;
        }
    }

    /**
     * Checks the card form fields: cardName, cardNumber, cardExpiry (MM/YY), cardCvv.
     * Adds a message to "errors" for every problem.
     *
     * @return the card's last 4 digits when everything is fine, otherwise null
     */
    public CardCheck checkTestCard(Map<String, String> form, List<String> errors) {
        List<String> found = new ArrayList<>();

        String name = TextUtil.clean(form.get("cardName"));
        if (name.length() < 2 || name.length() > 100) {
            found.add("Please enter the name on the card.");
        }

        String number = TextUtil.clean(form.get("cardNumber")).replaceAll("[\\s-]", "");
        if (!number.matches("\\d{13,19}") || !passesLuhn(number)) {
            found.add("The card number is not valid.");
        }

        String expiry = TextUtil.clean(form.get("cardExpiry"));
        YearMonth expiryMonth = parseExpiry(expiry);
        if (expiryMonth == null) {
            found.add("Card expiry must look like MM/YY.");
        } else if (expiryMonth.isBefore(YearMonth.now())) {
            found.add("This card has expired.");
        }

        String cvv = TextUtil.clean(form.get("cardCvv"));
        if (!cvv.matches("\\d{3,4}")) {
            found.add("The CVV is the 3 or 4 digits on the back of the card.");
        }

        errors.addAll(found);
        return found.isEmpty() ? new CardCheck(number.substring(number.length() - 4)) : null;
    }

    /** A reference for the receipt, e.g. PAY-20260917-4F7K2Q. */
    public String newReference() {
        String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(letters.charAt(RANDOM.nextInt(letters.length())));
        }
        return "PAY-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + code;
    }

    /** The Luhn check digit test that every real card number passes. */
    static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    private static YearMonth parseExpiry(String text) {
        if (!text.matches("(0[1-9]|1[0-2])\\s*/\\s*\\d{2}")) {
            return null;
        }
        String[] parts = text.split("/");
        return YearMonth.of(2000 + Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[0].trim()));
    }
}
