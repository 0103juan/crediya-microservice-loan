package co.com.pragma.usecase.registerloan;

import co.com.pragma.model.authuser.gateways.AuthUserRepository;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.exceptions.UserNotFoundException;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.state.State;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
public class RegisterLoanUseCase {

    private final LoanRepository loanRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final AuthUserRepository authRepository;

    public Mono<Loan> save(Loan loan, Integer loanTypeId) {
        return authRepository.findByEmail(loan.getUserEmail())
                .switchIfEmpty(Mono.error(new UserNotFoundException("El usuario " + loan.getUserEmail() + " no está registrado.")))
                .flatMap(authUser -> {
                    loan.setUserIdNumber(String.valueOf(authUser.getIdNumber()));
                    return loanTypeRepository.findById(loanTypeId)
                            .switchIfEmpty(Mono.error(new InvalidLoanTypeException("El tipo de préstamo con ID " + loanTypeId + " no existe.")));
                })
                .flatMap(loanType -> {
                    loan.setLoanType(loanType);
                    loan.setState(State.REVIEW_PENDING);
                    return loanRepository.save(loan);
                });
    }
}