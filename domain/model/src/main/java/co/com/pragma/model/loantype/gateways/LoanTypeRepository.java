package co.com.pragma.model.loantype.gateways;

import co.com.pragma.model.loantype.LoanType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LoanTypeRepository {
    Mono<LoanType> saveLoanType(LoanType loanType);
    Flux<LoanType> getAllLoansType();
    Mono<LoanType> getLoanTypeById(Integer loanTypeId);
}
