package co.com.pragma.model.loan.gateways;

import co.com.pragma.model.loan.Loan;
import reactor.core.publisher.Mono;

public interface LoanEventGateway {
    Mono<Void> publishDecision(Loan loan);
}
