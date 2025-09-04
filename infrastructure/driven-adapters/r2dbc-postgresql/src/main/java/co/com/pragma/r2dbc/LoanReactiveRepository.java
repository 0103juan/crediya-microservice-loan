package co.com.pragma.r2dbc;

import co.com.pragma.model.state.State;
import co.com.pragma.r2dbc.entity.LoanEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.List;

public interface LoanReactiveRepository extends ReactiveCrudRepository<LoanEntity, String>, ReactiveQueryByExampleExecutor<LoanEntity> {
    Flux<LoanEntity> findAllByStateIn(List<State> states);
}
