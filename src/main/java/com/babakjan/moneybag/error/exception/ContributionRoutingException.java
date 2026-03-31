package com.babakjan.moneybag.error.exception;

public class ContributionRoutingException extends Exception {

    private final String errorCode;

    public ContributionRoutingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ContributionRoutingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
