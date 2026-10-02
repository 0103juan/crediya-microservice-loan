package co.com.pragma.model.exceptions;

import lombok.Getter;


@Getter
public class LoanNotFoundException extends RuntimeException {
    private final Long loanId;

    public LoanNotFoundException(Long loanId) {
        super();
        this.loanId = loanId;
    }
}