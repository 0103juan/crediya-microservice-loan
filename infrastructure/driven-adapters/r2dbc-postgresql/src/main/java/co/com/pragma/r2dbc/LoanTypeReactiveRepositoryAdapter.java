package co.com.pragma.r2dbc;

import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.r2dbc.entity.LoanTypeEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
public class LoanTypeReactiveRepositoryAdapter extends ReactiveAdapterOperations<
    LoanType,
    LoanTypeEntity,
    Integer,
    LoanTypeReactiveRepository
> implements LoanTypeRepository {

    private final TransactionalOperator transactionalOperator;

    public LoanTypeReactiveRepositoryAdapter(LoanTypeReactiveRepository repository, ObjectMapper mapper, TransactionalOperator transactionalOperator) {
        super(repository, mapper, d -> mapper.map(d, LoanType.class));
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<LoanType> save(LoanType loanType) {
        log.info("Guardando nuevo tipo de préstamo: {}", loanType.getName());
        return super.save(loanType)
                .doOnSuccess(savedUser ->
                        log.info("Entidad de usuario guardada exitosamente en la base de datos."))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<LoanType> findById(Integer id) {
        log.info("Buscando tipo de préstamo con ID: {}", id);
        return super.findById(id)
                .doOnSuccess(savedUser ->
                        log.info("Entidad de tipo de préstamo encontrado exitosamente en la base de datos."));
    }

    @Override
    public Flux<LoanType> findAll() {
        log.info("Obteniendo todos los tipos de préstamo.");
        return super.findAll()
                .doOnComplete(() -> log.info("Entidades de tipos de préstamos encontrados exitosamente en la base de datos."));
    }
}
