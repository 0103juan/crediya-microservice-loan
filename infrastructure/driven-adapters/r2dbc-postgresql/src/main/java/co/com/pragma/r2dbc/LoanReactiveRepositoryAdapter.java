package co.com.pragma.r2dbc;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.state.State;
import co.com.pragma.r2dbc.entity.LoanEntity;
import co.com.pragma.r2dbc.entity.LoanTypeEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class LoanReactiveRepositoryAdapter extends ReactiveAdapterOperations<
    Loan,
    LoanEntity,
    String,
    LoanReactiveRepository
> implements LoanRepository {
    private final TransactionalOperator transactionalOperator;
    private final LoanTypeReactiveRepository loanTypeReactiveRepository;

    public LoanReactiveRepositoryAdapter(LoanReactiveRepository repository, ObjectMapper mapper, TransactionalOperator transactionalOperator, LoanTypeReactiveRepository loanTypeReactiveRepository) {
        super(repository, mapper, d -> mapper.map(d, Loan.class));
        this.transactionalOperator = transactionalOperator;
        this.loanTypeReactiveRepository = loanTypeReactiveRepository;
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
    public Mono<Loan> findById(Long idNumber) {
        return null;
    }

    @Override
    public Flux<Loan> findAll() {
        return null;
    }

    @Override
    public Flux<Loan> findAllByStateIn(List<State> states) {
        log.info("ADAPTER: Componiendo objetos de dominio 'Loan' para los estados: {}", states);

        Mono<Map<Integer, LoanType>> loanTypesMapMono = loanTypeReactiveRepository.findAll()
                .collect(Collectors.toMap(
                        LoanTypeEntity::getId,
                        loanTypeEntity -> mapper.map(loanTypeEntity, LoanType.class)
                ));

        return loanTypesMapMono.flatMapMany(loanTypesMap ->
                repository.findAllByStateIn(states)
                        .map(loanEntity -> {
                            Loan loanModel = toEntity(loanEntity);
                            LoanType loanType = loanTypesMap.getOrDefault(loanEntity.getLoanType(), null);
                            loanModel.setLoanType(loanType);

                            return loanModel;
                        })
        );
    }
}
