package co.com.pragma.model.exceptions;

import lombok.Getter;

@Getter
public class InvalidLoanTypeException extends RuntimeException {
    private final transient Object identifier; // Puede ser ID (Integer) o nombre (String)

    public InvalidLoanTypeException(Object identifier) {
        super();
        this.identifier = identifier;
    }
}