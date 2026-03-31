package com.babakjan.moneybag.error.exception;

public class ContributionValidationException extends Exception {
    public ContributionValidationException() {
    }

    public ContributionValidationException(String message) {
        super(message);
    }

    public ContributionValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContributionValidationException(Throwable cause) {
        super(cause);
    }

    public ContributionValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
