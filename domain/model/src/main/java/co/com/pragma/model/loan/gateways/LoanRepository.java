package co.com.pragma.model.loan.gateways;

import co.com.pragma.model.loan.Loan;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LoanRepository {
    Mono<Loan> save(Loan loan);
    Mono<Loan> get(Long idNumber);
    Flux<Loan> getAll();
}
