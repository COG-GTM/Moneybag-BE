package com.babakjan.moneybag.error.exception;

public class PlanNotFoundException extends Exception {
    public PlanNotFoundException() {
    }

    public PlanNotFoundException(String planId) {
        super("Plan configuration for planId: " + planId + " not found.");
    }

    public PlanNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlanNotFoundException(Throwable cause) {
        super(cause);
    }
}
