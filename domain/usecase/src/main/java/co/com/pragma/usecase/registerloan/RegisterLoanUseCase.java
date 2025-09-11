package co.com.pragma.usecase.registerloan;

import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.authuser.gateways.AuthUserRepository;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.exceptions.UserNotFoundException;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loantype.LoanType;
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
        Mono<AuthUser> authUserMono = authRepository.findByEmail(loan.getUserEmail())
                .switchIfEmpty(Mono.error(new UserNotFoundException(loan.getUserEmail())));

        Mono<LoanType> loanTypeMono = loanTypeRepository.findById(loanTypeId)
                .switchIfEmpty(Mono.error(new InvalidLoanTypeException(loanTypeId)));

        return authUserMono.zipWith(loanTypeMono)
                .flatMap(tuple -> {
                    AuthUser authUser = tuple.getT1();
                    LoanType loanType = tuple.getT2();

                    loan.setUserIdNumber(String.valueOf(authUser.getIdNumber()));
                    loan.setLoanType(loanType);

                    State finalState = loanType.isAutomaticValidation()
                            ? State.REVIEW_PENDING
                            : State.MANUAL_REVIEW;

                    loan.setState(finalState);

                    return loanRepository.save(loan);
                });
    }
}