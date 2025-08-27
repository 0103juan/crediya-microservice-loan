package co.com.pragma.r2dbc;

import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.r2dbc.entity.LoanTypeEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
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
    public LoanTypeReactiveRepositoryAdapter(LoanTypeReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, LoanType.class));
    }

    @Override
    public Mono<LoanType> saveLoanType(LoanType loanType) {
        log.info("Guardando nuevo tipo de préstamo: {}", loanType.getName());
        return save(loanType);
    }

    @Override
    public Mono<LoanType> getLoanTypeById(Integer id) {
        log.info("Buscando tipo de préstamo con ID: {}", id);
        return findById(id);
    }

    @Override
    public Flux<LoanType> getAllLoansType() {
        log.info("Obteniendo todos los tipos de préstamo.");
        return findAll();
    }
}
