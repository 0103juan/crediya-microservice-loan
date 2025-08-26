package co.com.pragma.model.exceptions;

import lombok.Getter;

import java.util.Map;

@Getter
public class LoanValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public LoanValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
}