package com.medisys.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown by a service when user input breaks a business rule. The servlet shows
 * the message(s) to the user.
 *
 * It can hold one message, or a list of messages when a whole form is checked
 * at once (so the user sees every problem together).
 *
 * Module : Shared - agree with the team before changing it
 * Owner  : Whole team
 */
public class ValidationException extends Exception {

    private final List<String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public ValidationException(List<String> errors) {
        super(String.join(" ", errors));
        this.errors = new ArrayList<>(errors);
    }

    /** Every problem found, one message per item. */
    public List<String> getErrors() {
        return errors;
    }
}
