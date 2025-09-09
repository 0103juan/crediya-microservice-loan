package co.com.pragma.usecase.findloans;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.pagequery.PageQuery;
import co.com.pragma.model.paginatedresult.PaginatedResult;
import co.com.pragma.model.state.State;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
public class FindLoansUseCase {

    private final LoanRepository loanRepository;

    public Mono<PaginatedResult<Loan>> findByStatus(PageQuery pageQuery) {
        // La lógica de negocio (qué estados buscar) vive aquí.
        List<State> statuses = List.of(State.REVIEW_PENDING, State.REJECTED); // Añade los estados que necesites

        return loanRepository.findAllByStateIn(statuses, pageQuery);
    }
}