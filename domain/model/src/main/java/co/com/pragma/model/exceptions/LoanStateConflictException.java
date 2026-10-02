package co.com.pragma.model.exceptions;

import co.com.pragma.model.state.State;
import lombok.Getter;

@Getter
public class LoanStateConflictException extends RuntimeException {
    private final Long loanId;
    private final State currentState;
    private final State requestedState;

    public LoanStateConflictException(Long loanId, State currentState, State requestedState) {
        super();
        this.loanId = loanId;
        this.currentState = currentState;
        this.requestedState = requestedState;
    }
}
