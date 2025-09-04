package co.com.pragma.usecase.findloans;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.state.State;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.List;

@RequiredArgsConstructor
public class FindLoansUseCase {

    private final LoanRepository loanRepository;

    public Flux<Loan> findByStatus() {
        List<State> statuses = List.of(State.REVIEW_PENDING, State.REJECTED, State.MANUAL_REVIEW);
        return loanRepository.findAllByStateIn(statuses);
    }
}