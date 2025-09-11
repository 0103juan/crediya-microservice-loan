package co.com.pragma.r2dbc.helper;

import co.com.pragma.model.paginatedresult.PaginatedResult;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.Example;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.function.Function;

public abstract class ReactiveAdapterOperations<
        E, // Domain entity
        D, // Data entity
        I, // ID type
        R extends ReactiveCrudRepository<D, I> & ReactiveQueryByExampleExecutor<D>
        > {

    protected final R repository;
    protected final ObjectMapper mapper;
    private final Class<D> dataClass;
    private final Function<D, E> toEntityFn;

    @SuppressWarnings("unchecked")
    protected ReactiveAdapterOperations(R repository, ObjectMapper mapper, Function<D, E> toEntityFn) {
        this.repository = repository;
        this.mapper = mapper;
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        this.dataClass = (Class<D>) genericSuperclass.getActualTypeArguments()[1];
        this.toEntityFn = toEntityFn;
    }

    // --- Mapping ---
    protected D toData(E entity) {
        return mapper.map(entity, dataClass);
    }

    protected E toEntity(D data) {
        return data != null ? enrich(toEntityFn.apply(data)) : null;
    }

    protected E enrich(E entity) {
        return entity;
    }

    // --- Save ---
    public Mono<E> save(E entity) {
        return saveData(toData(entity))
                .map(this::toEntity);
    }

    protected Flux<E> saveAllEntities(Flux<E> entities) {
        return saveData(entities.map(this::toData))
                .map(this::toEntity);
    }

    protected Mono<D> saveData(D data) {
        return repository.save(data);
    }

    protected Flux<D> saveData(Flux<D> data) {
        return repository.saveAll(data);
    }

    // --- Queries ---
    public Mono<E> findById(I id) {
        return repository.findById(id).map(this::toEntity);
    }

    public Flux<E> findByExample(E entity) {
        return repository.findAll(Example.of(toData(entity)))
                .map(this::toEntity);
    }

    public Flux<E> findAll() {
        return repository.findAll()
                .map(this::toEntity);
    }

    // --- Paginación genérica ---
    public Mono<PaginatedResult<E>> findAllPaged(Flux<D> query, Mono<Long> count, int page, int size) {
        return Mono.zip(
                query.map(this::toEntity).collectList(),
                count
        ).map(tuple -> {
            List<E> content = tuple.getT1();
            long total = tuple.getT2();
            int totalPages = (int) Math.ceil((double) total / size);
            return new PaginatedResult<>(content, total, totalPages, page, size);
        });
    }
}
