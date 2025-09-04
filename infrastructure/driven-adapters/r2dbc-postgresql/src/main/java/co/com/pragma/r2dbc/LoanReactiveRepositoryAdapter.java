package co.com.pragma.r2dbc;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.r2dbc.entity.LoanEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
public class LoanReactiveRepositoryAdapter extends ReactiveAdapterOperations<
    Loan,
    LoanEntity,
    String,
    LoanReactiveRepository
> implements LoanRepository {
    private final TransactionalOperator transactionalOperator;

    public LoanReactiveRepositoryAdapter(LoanReactiveRepository repository, ObjectMapper mapper, TransactionalOperator transactionalOperator) {
        super(repository, mapper, d -> mapper.map(d, Loan.class));
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<Loan> save(Loan loan) {
        log.info("Iniciando operación de guardado para la solicitud de préstamo con email: {}", loan.getUserEmail());
        return super.save(loan)
                .doOnSuccess(savedLoan ->
                        log.info("Entidad de préstamo guardada exitosamente en la base de datos."))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Loan> get(Long idNumber) {
        return null;
    }

    @Override
    public Flux<Loan> getAll() {
        return null;
    }
}
