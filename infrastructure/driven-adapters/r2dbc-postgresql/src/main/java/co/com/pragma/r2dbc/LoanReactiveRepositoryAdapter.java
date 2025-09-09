package co.com.pragma.r2dbc;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.pagequery.PageQuery;
import co.com.pragma.model.paginatedresult.PaginatedResult;
import co.com.pragma.model.state.State;
import co.com.pragma.r2dbc.entity.LoanEntity;
import co.com.pragma.r2dbc.entity.LoanTypeEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final Mono<Map<Integer, LoanType>> loanTypesCache;

    public LoanReactiveRepositoryAdapter(
            LoanReactiveRepository repository,
            ObjectMapper mapper,
            TransactionalOperator transactionalOperator,
            LoanTypeReactiveRepositoryAdapter loanTypeReactiveRepositoryAdapter
    ) {
        super(repository, mapper, d -> mapper.map(d, Loan.class));
        this.transactionalOperator = transactionalOperator;
        this.loanTypesCache = loanTypeReactiveRepositoryAdapter.findAllAsMap().cache();
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
    public Mono<PaginatedResult<Loan>> findAllByStateIn(List<State> statuses, PageQuery pageQuery) {
        Pageable pageable = PageRequest.of(pageQuery.page(), pageQuery.size());

        return findAllPaged(
                repository.findByStateIn(statuses, pageable),
                repository.countByStateIn(statuses),
                pageQuery
        );
    }


    @Override
    protected Loan enrich(Loan loan) {
        // Usamos el cache reactivo
        loanTypesCache.subscribe(loanTypesMap -> {
            // loan.getLoanTypeId() debe venir del LoanEntity mapeado
            Integer loanTypeId = loan.getLoanTypeId();
            if (loanTypeId != null) {
                LoanType foundLoanType = loanTypesMap.get(loanTypeId);
                if (foundLoanType != null) {
                    loan.setLoanType(foundLoanType);
                }
            }
        });
        return loan;
    }
}
