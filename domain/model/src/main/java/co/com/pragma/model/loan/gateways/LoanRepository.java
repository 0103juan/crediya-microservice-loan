package co.com.pragma.model.loan.gateways;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loanquery.LoanQuery;
import co.com.pragma.model.paginatedresult.PaginatedResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoanRepository {
    Mono<Loan> save(Loan loan);
    Mono<Loan> findById(Long idNumber);
    Flux<Loan> findAll();
    Mono<PaginatedResult<Loan>> findByQuery(LoanQuery query);
}
