package com.babakjan.moneybag.error.exception;

public class TaxReportingException extends Exception {

    public TaxReportingException() {
    }

    public TaxReportingException(String message) {
        super(message);
    }

    public TaxReportingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaxReportingException(Throwable cause) {
        super(cause);
    }
}
