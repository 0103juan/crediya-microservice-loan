package co.com.pragma.model.exceptions;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class LoanValidationException extends RuntimeException {

    private final Map<String, List<String>> errors;

    public LoanValidationException(Map<String, List<String>> errors) {
        super();
        this.errors = errors;
    }
}