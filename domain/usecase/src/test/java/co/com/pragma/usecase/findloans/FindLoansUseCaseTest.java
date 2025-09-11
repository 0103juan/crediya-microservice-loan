package co.com.pragma.usecase.findloans;

import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.authuser.gateways.AuthUserRepository;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loandetail.LoanDetail;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.loanquery.LoanQuery;
import co.com.pragma.model.paginatedresult.PaginatedResult;
import co.com.pragma.model.state.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindLoansUseCaseTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private AuthUserRepository authUserRepository;
    @Mock
    private LoanTypeRepository loanTypeRepository;

    @InjectMocks
    private FindLoansUseCase findLoansUseCase;

    private Loan loan;
    private AuthUser authUser;
    private LoanType loanType;
    private PaginatedResult<Loan> paginatedResult;

    @BeforeEach
    void setUp() {
        loan = Loan.builder()
                .amount(BigDecimal.valueOf(25000))
                .term(36)
                .userEmail("test@pragma.com")
                .userIdNumber("123456789")
                .loanTypeId(1)
                .state(State.MANUAL_REVIEW)
                .build();

        authUser = AuthUser.builder()
                .email("test@pragma.com")
                .firstName("Test")
                .lastName("User")
                .idNumber(123456789L)
                .baseSalary(BigDecimal.valueOf(50000))
                .build();

        loanType = LoanType.builder()
                .id(1)
                .name("VEHICULO")
                .interestRate(BigDecimal.valueOf(0.08))
                .build();

        paginatedResult = new PaginatedResult<>(
                List.of(loan),
                1,
                1,
                0,
                10
        );
    }

    @Test
    @DisplayName("Debería encontrar y devolver los detalles del préstamo paginados correctamente")
    void findByStatus_Success() {
        LoanQuery query = LoanQuery.builder().page(0).size(10).build();

        when(loanRepository.findByQuery(any(LoanQuery.class))).thenReturn(Mono.just(paginatedResult));
        when(authUserRepository.findAllByEmails(List.of("test@pragma.com"))).thenReturn(Flux.just(authUser));
        when(loanTypeRepository.findAllByIds(List.of(1))).thenReturn(Flux.just(loanType));

        StepVerifier.create(findLoansUseCase.findByStatus(query))
                .expectNextMatches(result -> {
                    if (result.content().size() != 1) return false;
                    LoanDetail detail = result.content().getFirst();
                    return detail.getLoan().equals(loan) &&
                            detail.getUser().equals(authUser) &&
                            detail.getLoanType().equals(loanType);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Debería devolver un resultado vacío si no se encuentran préstamos")
    void findByStatus_whenNoLoansFound_shouldReturnEmpty() {
        LoanQuery query = LoanQuery.builder().page(0).size(10).build();
        PaginatedResult<Loan> emptyPaginatedResult = new PaginatedResult<>(Collections.emptyList(), 0, 0, 0, 10);

        when(loanRepository.findByQuery(any(LoanQuery.class))).thenReturn(Mono.just(emptyPaginatedResult));

        StepVerifier.create(findLoansUseCase.findByStatus(query))
                .expectNextMatches(result -> result.content().isEmpty() && result.totalElements() == 0)
                .verifyComplete();
    }
}