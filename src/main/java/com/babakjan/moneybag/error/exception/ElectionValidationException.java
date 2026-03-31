package com.babakjan.moneybag.error.exception;

import lombok.Getter;

@Getter
public class ElectionValidationException extends Exception {

    private final String errorCode;
    private final String errorMessage;

    public ElectionValidationException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public ElectionValidationException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
