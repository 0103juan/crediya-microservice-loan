package co.com.pragma.usecase.updateloanstatus;

import co.com.pragma.model.exceptions.LoanNotFoundException;
import co.com.pragma.model.exceptions.LoanStateConflictException;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loan.gateways.NotificationGateway;
import co.com.pragma.model.state.State;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateLoanStatusUseCaseTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private NotificationGateway notificationGateway;

    @InjectMocks
    private UpdateLoanStatusUseCase updateLoanStatusUseCase;

    private Loan loanIn(State state) {
        return Loan.builder()
                .id(7L)
                .amount(BigDecimal.valueOf(30000000))
                .term(48)
                .userEmail("cliente@test.com")
                .state(state)
                .build();
    }

    @Test
    @DisplayName("Aprobar una solicitud pendiente la guarda con el mismo id y notifica al cliente")
    void approve_savesTheSameLoanAndNotifies() {
        when(loanRepository.findById(7L)).thenReturn(Mono.just(loanIn(State.MANUAL_REVIEW)));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(notificationGateway.sendLoanStatusNotification(any(Loan.class))).thenReturn(Mono.empty());

        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(7L, State.APPROVED))
                .expectNextMatches(loan -> loan.getId().equals(7L) && loan.getState() == State.APPROVED)
                .verifyComplete();

        verify(notificationGateway).sendLoanStatusNotification(any(Loan.class));
    }

    @Test
    @DisplayName("Una solicitud que no existe no se guarda ni se notifica")
    void unknownLoan_isNotFound() {
        when(loanRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(99L, State.APPROVED))
                .expectError(LoanNotFoundException.class)
                .verify();

        verify(loanRepository, never()).save(any());
        verify(notificationGateway, never()).sendLoanStatusNotification(any());
    }

    @Test
    @DisplayName("Una solicitud ya decidida no se puede decidir otra vez")
    void decidedLoan_cannotBeDecidedAgain() {
        when(loanRepository.findById(7L)).thenReturn(Mono.just(loanIn(State.APPROVED)));

        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(7L, State.REJECTED))
                .expectError(LoanStateConflictException.class)
                .verify();

        verify(loanRepository, never()).save(any());
        verify(notificationGateway, never()).sendLoanStatusNotification(any());
    }

    @Test
    @DisplayName("Solo se puede aprobar o rechazar: devolver una solicitud a revisión no es una decisión")
    void onlyApprovedOrRejectedAreDecisions() {
        when(loanRepository.findById(7L)).thenReturn(Mono.just(loanIn(State.REVIEW_PENDING)));

        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(7L, State.MANUAL_REVIEW))
                .expectError(LoanStateConflictException.class)
                .verify();

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si la cola falla, el error llega a quien llamó")
    void whenTheQueueFails_theErrorIsNotSwallowed() {
        when(loanRepository.findById(7L)).thenReturn(Mono.just(loanIn(State.MANUAL_REVIEW)));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(notificationGateway.sendLoanStatusNotification(any(Loan.class)))
                .thenReturn(Mono.error(new IllegalStateException("cola caída")));

        StepVerifier.create(updateLoanStatusUseCase.updateLoanStatus(7L, State.REJECTED))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
