package co.com.pragma.model.loan.gateways;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.state.State;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoanRepository {
    Mono<Loan> save(Loan loan);
    Mono<Loan> findById(Long idNumber);
    Flux<Loan> findAll();
    Flux<Loan> findAllByStateIn(List<State> states);
}
