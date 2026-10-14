package co.com.pragma.model.exceptions;

import lombok.Getter;

@Getter
public class LoanConcurrentUpdateException extends RuntimeException {
    private final Long loanId;

    public LoanConcurrentUpdateException(Long loanId) {
        super();
        this.loanId = loanId;
    }
}
