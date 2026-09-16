package com.medisys.service;

/**
 * Thrown by a service when user input breaks a business rule. The servlet shows the message to the user.
 *
 * Module : Shared - agree with the team before changing
 * Owner  : Whole team
 *
 * TODO: nothing - ready to use.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}
