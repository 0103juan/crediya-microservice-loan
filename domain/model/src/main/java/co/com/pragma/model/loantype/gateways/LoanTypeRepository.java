package co.com.pragma.model.loantype.gateways;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loantype.LoanType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LoanTypeRepository {
    Mono<LoanType> saveLoanType(LoanType loanType);
    Mono<LoanType> getLoanType(Long idLoan);
    Flux<LoanType> getAllLoansType();
}
