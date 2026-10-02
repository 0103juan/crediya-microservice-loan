package co.com.pragma.usecase.updateloanstatus;

import co.com.pragma.model.exceptions.LoanNotFoundException;
import co.com.pragma.model.exceptions.LoanStateConflictException;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loan.gateways.NotificationGateway;
import co.com.pragma.model.state.State;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
public class UpdateLoanStatusUseCase {

    private final LoanRepository loanRepository;
    private final NotificationGateway notificationGateway;

    public Mono<Loan> updateLoanStatus(Long loanId, State newState) {
        return loanRepository.findById(loanId)
                .switchIfEmpty(Mono.error(new LoanNotFoundException(loanId)))
                .flatMap(loan -> {
                    // Una solicitud se decide una sola vez: solo se aprueba o rechaza lo que sigue pendiente.
                    if (!newState.isDecision() || loan.getState().isDecision()) {
                        return Mono.error(new LoanStateConflictException(loanId, loan.getState(), newState));
                    }
                    loan.setState(newState);
                    return loanRepository.save(loan);
                })
                .flatMap(savedLoan -> notificationGateway.sendLoanStatusNotification(savedLoan).thenReturn(savedLoan));
    }
}
