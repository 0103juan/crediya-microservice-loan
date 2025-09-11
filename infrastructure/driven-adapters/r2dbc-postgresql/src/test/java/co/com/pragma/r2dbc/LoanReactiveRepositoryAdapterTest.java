package co.com.pragma.r2dbc;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loanquery.LoanQuery;
import co.com.pragma.model.paginatedresult.PaginatedResult;
import co.com.pragma.model.state.State;
import co.com.pragma.r2dbc.entity.LoanEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanReactiveRepositoryAdapterTest {

    @Mock
    private LoanReactiveRepository repository;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private TransactionalOperator transactionalOperator;
    @Mock
    private R2dbcEntityTemplate entityTemplate;

    @InjectMocks
    private LoanReactiveRepositoryAdapter repositoryAdapter;

    private Loan loan;
    private LoanEntity loanEntity;

    @BeforeEach
    void setup() {
        loan = new Loan();
        loan.setAmount(BigDecimal.valueOf(15000));
        loan.setTerm(36);
        loan.setUserIdNumber("11223344");
        loan.setUserEmail("repo@test.com");
        loan.setLoanTypeId(3);
        loan.setState(State.REVIEW_PENDING);


        loanEntity = new LoanEntity(
                BigInteger.ONE,
                loan.getAmount(),
                loan.getTerm(),
                loan.getUserEmail(),
                loan.getUserIdNumber(),
                loan.getLoanTypeId(),
                loan.getState()
        );
    }

    @Test
    void saveLoan_Success() {
        when(mapper.map(any(Loan.class), any(Class.class))).thenReturn(loanEntity);
        when(repository.save(any(LoanEntity.class))).thenReturn(Mono.just(loanEntity));
        when(mapper.map(any(LoanEntity.class), any(Class.class))).thenReturn(loan);
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));


        Mono<Loan> result = repositoryAdapter.save(loan);

        StepVerifier.create(result)
                .expectNextMatches(savedLoan -> savedLoan.getUserEmail().equals("repo@test.com"))
                .verifyComplete();
    }

    @Test
    void findByQuery_Success() {
        // Arrange
        LoanQuery query = LoanQuery.builder()
                .page(0)
                .size(10)
                .states(List.of(State.MANUAL_REVIEW))
                .userEmail(Optional.of("test@example.com"))
                .userIdNumber(Optional.of("12345"))
                .build();

        when(entityTemplate.select(any(Query.class), eq(LoanEntity.class))).thenReturn(Flux.just(loanEntity));
        when(entityTemplate.count(any(Query.class), eq(LoanEntity.class))).thenReturn(Mono.just(1L));
        when(mapper.map(loanEntity, Loan.class)).thenReturn(loan);

        // Act
        Mono<PaginatedResult<Loan>> result = repositoryAdapter.findByQuery(query);

        // Assert
        StepVerifier.create(result)
                .expectNextMatches(paginatedResult ->
                        paginatedResult.totalElements() == 1 &&
                                paginatedResult.content().getFirst().getUserEmail().equals("repo@test.com")
                )
                .verifyComplete();
    }
}