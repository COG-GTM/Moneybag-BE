package com.babakjan.moneybag.error.exception;

import lombok.Getter;

@Getter
public class PayrollProcessingException extends Exception {
    private final String errorCode;

    public PayrollProcessingException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
    }

    public PayrollProcessingException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
    }
}
