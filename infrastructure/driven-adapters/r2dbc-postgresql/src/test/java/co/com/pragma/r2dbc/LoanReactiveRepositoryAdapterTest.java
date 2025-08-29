package co.com.pragma.r2dbc;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.state.State;
import co.com.pragma.r2dbc.entity.LoanEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanReactiveRepositoryAdapterTest {

    @Mock
    private LoanReactiveRepository repository;
    @Mock
    private ObjectMapper mapper;
    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private LoanReactiveRepositoryAdapter repositoryAdapter;

    private Loan loan;
    private LoanEntity loanEntity;

    @BeforeEach
    void setup() {
        loan = new Loan(
                BigDecimal.valueOf(15000),
                36,
                "11223344",
                "repo@test.com",
                3,
                State.REVIEW_PENDING
        );

        loanEntity = new LoanEntity(
                BigInteger.ONE,
                loan.getAmount(),
                loan.getTerm(),
                loan.getUserEmail(),
                loan.getUserIdNumber(),
                loan.getLoanType(),
                loan.getState()
        );
    }

    @Test
    void saveLoan_Success() {
        when(mapper.map(any(Loan.class), any(Class.class))).thenReturn(loanEntity);
        when(repository.save(any(LoanEntity.class))).thenReturn(Mono.just(loanEntity));
        when(mapper.map(any(LoanEntity.class), any(Class.class))).thenReturn(loan);
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));


        Mono<Loan> result = repositoryAdapter.saveLoan(loan);

        StepVerifier.create(result)
                .expectNextMatches(savedLoan -> savedLoan.getUserEmail().equals("repo@test.com"))
                .verifyComplete();
    }
}