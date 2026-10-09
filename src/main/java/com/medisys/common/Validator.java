package com.medisys.common;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * VALIDATION: the input checks that more than one form needs.
 *
 * Each method checks one field. When the value is wrong it adds a message to
 * the "errors" list (so the user sees every problem at once) and the servlet
 * shows the form again. Checks that only one form needs are written in that
 * servlet's own validate...() method.
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public final class Validator {

    public static final int PASSWORD_MIN = 8;
    /** Customers must be at least this old to register. */
    public static final int MIN_AGE = 18;

    private Validator() {
    }

    /** A person's name: letters, spaces, . ' - and 2 to 100 characters. */
    public static String name(String text, List<String> errors) {
        String name = TextUtil.clean(text).replaceAll("\\s+", " ");
        if (name.length() < 2 || name.length() > 100 || !name.matches("[\\p{L} .'-]+")) {
            errors.add("Please enter your full name (letters only, 2 to 100 characters).");
        }
        return name;
    }

    /** True for a normal e-mail address (something@something.xx). */
    public static boolean isEmail(String email) {
        return email.matches("[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}") && email.length() <= 150;
    }

    /**
     * Checks a Sri Lankan phone number and returns it as 0771234567.
     * +94 771234567, 077-123 4567 and similar are accepted.
     *
     * @param required   an empty value is an error
     * @param mobileOnly must be a mobile number (07...), e.g. for WhatsApp
     * @return the number, or null when it is empty or wrong
     */
    public static String phone(String text, String label, boolean required, boolean mobileOnly, List<String> errors) {
        String digits = TextUtil.clean(text).replaceAll("[\\s\\-()]", "");
        if (digits.isEmpty()) {
            if (required) {
                errors.add("Please enter your " + label + ".");
            }
            return null;
        }
        if (digits.startsWith("+94")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("94") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        String pattern = mobileOnly ? "07\\d{8}" : "0\\d{9}";
        if (!digits.matches(pattern)) {
            errors.add("Please enter a valid " + label
                    + (mobileOnly ? " (a mobile number, e.g. 0771234567)." : ", e.g. 0771234567."));
            return null;
        }
        return digits;
    }

    /** At least 8 characters with a letter and a number, and typed the same twice. */
    public static void password(String password, String confirm, List<String> errors) {
        if (password == null || password.length() < PASSWORD_MIN || password.length() > 100
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            errors.add("The password needs at least " + PASSWORD_MIN
                    + " characters, with at least one letter and one number.");
        } else if (!password.equals(confirm)) {
            errors.add("The two passwords do not match.");
        }
    }

    /**
     * The birth year hidden in a Sri Lankan NIC, or null if the NIC is not valid.
     *   old: YYDDDnnnnV   -> 19YY      new: YYYYDDDnnnnn -> YYYY
     * DDD is the day of the year (500 is added for women), so it must be 1-366 or 501-866.
     */
    public static Integer nicBirthYear(String nic) {
        int year;
        int day;
        if (nic.matches("\\d{9}[VX]")) {
            year = 1900 + Integer.parseInt(nic.substring(0, 2));
            day = Integer.parseInt(nic.substring(2, 5));
        } else if (nic.matches("\\d{12}")) {
            year = Integer.parseInt(nic.substring(0, 4));
            day = Integer.parseInt(nic.substring(4, 7));
        } else {
            return null;
        }
        if (day > 500) {
            day -= 500;
        }
        return day >= 1 && day <= 366 ? year : null;
    }

    /**
     * Checks the TEST card fields: cardName, cardNumber, cardExpiry (MM/YY), cardCvv.
     * No real money moves - any card in a valid shape is accepted. The full card
     * number and the CVV are never stored, only the last 4 digits.
     *
     * @return the card's last 4 digits, or null when something is wrong
     */
    public static String card(Map<String, String> form, List<String> errors) {
        int before = errors.size();

        String name = TextUtil.clean(form.get("cardName"));
        if (name.length() < 2 || name.length() > 100) {
            errors.add("Please enter the name on the card.");
        }

        String number = TextUtil.clean(form.get("cardNumber")).replaceAll("[\\s-]", "");
        if (!number.matches("\\d{13,19}") || !passesLuhn(number)) {
            errors.add("The card number is not valid.");
        }

        String expiry = TextUtil.clean(form.get("cardExpiry"));
        if (!expiry.matches("(0[1-9]|1[0-2])\\s*/\\s*\\d{2}")) {
            errors.add("Card expiry must look like MM/YY.");
        } else {
            String[] parts = expiry.split("/");
            YearMonth month = YearMonth.of(2000 + Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[0].trim()));
            if (month.isBefore(YearMonth.now())) {
                errors.add("This card has expired.");
            }
        }

        if (!TextUtil.clean(form.get("cardCvv")).matches("\\d{3,4}")) {
            errors.add("The CVV is the 3 or 4 digits on the back of the card.");
        }
        return errors.size() == before ? number.substring(number.length() - 4) : null;
    }

    /** The Luhn check digit test that every real card number passes. */
    private static boolean passesLuhn(String digits) {
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
}
