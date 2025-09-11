package co.com.pragma.model.exceptions;

import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {
    private final String userIdentifier;

    public UserNotFoundException(String userIdentifier) {
        super(); // El mensaje se construirá en el handler
        this.userIdentifier = userIdentifier;
    }
}