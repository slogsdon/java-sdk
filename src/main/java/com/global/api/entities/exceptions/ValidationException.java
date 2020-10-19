package com.global.api.entities.exceptions;

import java.util.List;

public class ValidationException extends ApiException {
    private final List<String> validationErrors;

    public ValidationException(List<String> validationErrors) {
        super("The application failed validation. Please see the validation errors for specific details.");
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
