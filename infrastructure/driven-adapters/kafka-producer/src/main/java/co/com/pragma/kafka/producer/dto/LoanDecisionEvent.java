package co.com.pragma.kafka.producer.dto;

import co.com.pragma.model.loan.Loan;

import java.math.BigDecimal;

public record LoanDecisionEvent(Long loanId, String userEmail, String status, BigDecimal amount, Integer term) {

    public static LoanDecisionEvent from(Loan loan) {
        return new LoanDecisionEvent(loan.getId(), loan.getUserEmail(), loan.getState().name(),
                loan.getAmount(), loan.getTerm());
    }
}
