package co.com.pragma.r2dbc;

import co.com.pragma.model.state.State;
import co.com.pragma.r2dbc.entity.LoanEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface LoanReactiveRepository extends ReactiveCrudRepository<LoanEntity, String>, ReactiveQueryByExampleExecutor<LoanEntity> {
    Flux<LoanEntity> findByState(State state, Pageable pageable);
    Mono<Long> countByState(State state);

    Flux<LoanEntity> findByStateIn(List<State> statuses, Pageable pageable);
    Mono<Long> countByStateIn(List<State> statuses);
}
