package co.com.pragma.model.loantype.gateways;

import co.com.pragma.model.loantype.LoanType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoanTypeRepository {
    Mono<LoanType> save(LoanType loanType);
    Flux<LoanType> findAll();
    Mono<LoanType> findById(Integer loanTypeId);
    Flux<LoanType> findAllByIds(List<Integer> loanTypeIds);
}
