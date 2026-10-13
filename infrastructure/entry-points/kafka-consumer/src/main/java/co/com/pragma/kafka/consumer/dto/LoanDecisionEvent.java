package co.com.pragma.kafka.consumer.dto;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.state.State;

import java.math.BigDecimal;
import java.util.Objects;

public record LoanDecisionEvent(Long loanId, String userEmail, State status, BigDecimal amount, Integer term) {

    // Sin estos tres campos no hay a quién ni qué notificar: Jackson lo reporta como un mensaje ilegible.
    public LoanDecisionEvent {
        Objects.requireNonNull(loanId, "loanId");
        Objects.requireNonNull(userEmail, "userEmail");
        Objects.requireNonNull(status, "status");
    }

    public Loan toLoan() {
        return Loan.builder().id(loanId).userEmail(userEmail).state(status).amount(amount).term(term).build();
    }
}
