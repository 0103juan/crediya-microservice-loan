package co.com.pragma.r2dbc;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loanquery.LoanQuery;
import co.com.pragma.model.paginatedresult.PaginatedResult;
import co.com.pragma.model.state.State;
import co.com.pragma.r2dbc.entity.LoanEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class LoanReactiveRepositoryAdapter extends ReactiveAdapterOperations<
    Loan,
    LoanEntity,
    String,
    LoanReactiveRepository
        > implements LoanRepository {
    private final TransactionalOperator transactionalOperator;
    private final R2dbcEntityTemplate entityTemplate;

    public LoanReactiveRepositoryAdapter(
            LoanReactiveRepository repository,
            ObjectMapper mapper,
            TransactionalOperator transactionalOperator,
            R2dbcEntityTemplate entityTemplate
    ) {
        super(repository, mapper, d -> mapper.map(d, Loan.class));
        this.transactionalOperator = transactionalOperator;
        this.entityTemplate = entityTemplate; // <- Asignar
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
    public Mono<PaginatedResult<Loan>> findByQuery(LoanQuery query) {
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());

        Criteria criteria = Criteria.where("id_state").in(query.getStates().stream().map(State::getId).toList());

        Optional<String> userEmailOpt = query.getUserEmail();
        if (userEmailOpt.isPresent() && !userEmailOpt.get().isBlank()) {
            criteria = criteria.and("user_email").like("%" + userEmailOpt.get() + "%").ignoreCase(true);
        }

        Optional<String> userIdNumberOpt = query.getUserIdNumber();
        if (userIdNumberOpt.isPresent() && !userIdNumberOpt.get().isBlank()) {
            criteria = criteria.and("user_id_number").like("%" + userIdNumberOpt.get() + "%").ignoreCase(true);
        }

        Query dbQuery = Query.query(criteria).with(pageable);
        Flux<Loan> contentFlux = entityTemplate.select(dbQuery, LoanEntity.class)
                .map(this::toEntity);

        Mono<Long> countMono = entityTemplate.count(Query.query(criteria), LoanEntity.class);

        return Mono.zip(contentFlux.collectList(), countMono)
                .map(tuple -> {
                    List<Loan> content = tuple.getT1();
                    long totalElements = tuple.getT2();
                    int totalPages = (query.getSize() == 0) ? 1 : (int) Math.ceil((double) totalElements / (double) query.getSize());

                    return new PaginatedResult<>(
                            content,
                            totalElements,
                            totalPages,
                            query.getPage(),
                            query.getSize()
                    );
                });
    }
}
