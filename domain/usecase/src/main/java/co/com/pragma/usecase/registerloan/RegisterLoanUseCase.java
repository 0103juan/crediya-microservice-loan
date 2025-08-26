package co.com.pragma.usecase.registerloan;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RegisterLoanUseCase {

    private final LoanRepository userRepository;

    public Mono<Loan> saveLoan(Loan user){
        return  userRepository.saveLoan(user)
                .cast(Loan.class);
    }
}
