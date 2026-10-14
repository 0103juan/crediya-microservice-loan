package co.com.pragma;

import co.com.pragma.model.exceptions.LoanConcurrentUpdateException;
import co.com.pragma.model.exceptions.LoanStateConflictException;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanEventGateway;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loanquery.LoanQuery;
import co.com.pragma.model.paginatedresult.PaginatedResult;
import co.com.pragma.model.state.State;
import co.com.pragma.usecase.updateloanstatus.UpdateLoanStatusUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The whole service wired as in production, against a PostgreSQL that Testcontainers starts and throws away.
// Kafka is left out: its adapter is a mock, and no topic is created and no consumer thread starts.
@SpringBootTest(properties = {
        "jwt.secret=integration-test-secret-0123456789-abcdef",
        "app.db.init.enabled=true",
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false"})
@Testcontainers(disabledWithoutDocker = true)
class LoanServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("adapters.r2dbc.host", POSTGRES::getHost);
        registry.add("adapters.r2dbc.port", POSTGRES::getFirstMappedPort);
        registry.add("adapters.r2dbc.database", POSTGRES::getDatabaseName);
        registry.add("adapters.r2dbc.username", POSTGRES::getUsername);
        registry.add("adapters.r2dbc.password", POSTGRES::getPassword);
    }

    @MockitoBean
    private LoanEventGateway loanEventGateway;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private UpdateLoanStatusUseCase updateLoanStatusUseCase;

    // The tests share one database, so each one works on rows of a client nobody else uses.
    private Loan loanOf(String userEmail, State state) {
        return Loan.builder().amount(new BigDecimal("30000000.00")).term(48).userEmail(userEmail)
                .userIdNumber("1061000001").loanTypeId(2).state(state).build();
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@test.com";
    }

    @Test
    void aSavedLoanGetsAnIdAndComesBackWithItsState() {
        Loan saved = loanRepository.save(loanOf(uniqueEmail(), State.MANUAL_REVIEW)).block();

        assertThat(saved.getId()).isNotNull();
        Loan found = loanRepository.findById(saved.getId()).block();
        assertThat(found.getState()).isEqualTo(State.MANUAL_REVIEW);
        assertThat(found.getAmount()).isEqualByComparingTo("30000000");
        assertThat(found.getUserEmail()).isEqualTo(saved.getUserEmail());
        assertThat(found.getLoanTypeId()).isEqualTo(2);
    }

    @Test
    void theAdviserQueryFiltersByStateAndEmailAndCountsEveryMatch() {
        String client = uniqueEmail();
        loanRepository.save(loanOf(client, State.MANUAL_REVIEW)).block();
        loanRepository.save(loanOf(client, State.MANUAL_REVIEW)).block();
        loanRepository.save(loanOf(client, State.APPROVED)).block();
        loanRepository.save(loanOf(uniqueEmail(), State.MANUAL_REVIEW)).block();

        // One row per page, and the email in another case: the filter is a case-insensitive "contains".
        PaginatedResult<Loan> page = loanRepository.findByQuery(LoanQuery.builder()
                .page(0).size(1)
                .states(List.of(State.MANUAL_REVIEW))
                .userEmail(Optional.of(client.toUpperCase()))
                .userIdNumber(Optional.empty())
                .build()).block();

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().getUserEmail()).isEqualTo(client);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void aStaleCopyOfALoanCannotOverwriteANewerOne() {
        Long id = loanRepository.save(loanOf(uniqueEmail(), State.MANUAL_REVIEW)).block().getId();
        Loan oneAdviser = loanRepository.findById(id).block();
        Loan anotherAdviser = loanRepository.findById(id).block();

        oneAdviser.setState(State.APPROVED);
        loanRepository.save(oneAdviser).block();
        anotherAdviser.setState(State.REJECTED);

        assertThatThrownBy(() -> loanRepository.save(anotherAdviser).block())
                .isInstanceOf(LoanConcurrentUpdateException.class);
        assertThat(loanRepository.findById(id).block().getState()).isEqualTo(State.APPROVED);
    }

    @Test
    void ofTwoAdvisersDecidingTheSameLoanAtOnce_onlyOneDecisionIsSavedAndPublished() {
        when(loanEventGateway.publishDecision(any(Loan.class))).thenReturn(Mono.empty());
        int rounds = 20;

        for (int round = 0; round < rounds; round++) {
            Long id = loanRepository.save(loanOf(uniqueEmail(), State.MANUAL_REVIEW)).block().getId();

            // Both are subscribed before either answers, so both usually read the loan while it is pending.
            List<String> outcomes = Flux.merge(decide(id, State.APPROVED), decide(id, State.REJECTED))
                    .collectList().block();

            assertThat(outcomes).containsExactlyInAnyOrder("decided", "conflict");
        }
        verify(loanEventGateway, times(rounds)).publishDecision(any(Loan.class));
    }

    // The loser is stopped by the version column, or by the state check if it happened to read after the winner wrote.
    private Mono<String> decide(Long id, State decision) {
        return updateLoanStatusUseCase.updateLoanStatus(id, decision)
                .map(loan -> "decided")
                .onErrorResume(e -> e instanceof LoanConcurrentUpdateException || e instanceof LoanStateConflictException,
                        e -> Mono.just("conflict"));
    }
}
