package com.sicms.exception;

public class AccountDisabledException extends AuthException {
    public AccountDisabledException(String message) {
        super(message);
    }
}
