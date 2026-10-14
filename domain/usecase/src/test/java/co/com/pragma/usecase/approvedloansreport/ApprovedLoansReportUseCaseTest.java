package co.com.pragma.usecase.approvedloansreport;

import co.com.pragma.model.approvedloansreport.ApprovedLoansReport;
import co.com.pragma.model.approvedloansreport.gateways.ApprovedLoansReportRepository;
import co.com.pragma.model.loan.Loan;
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
class ApprovedLoansReportUseCaseTest {

    @Mock
    private ApprovedLoansReportRepository approvedLoansReportRepository;

    @InjectMocks
    private ApprovedLoansReportUseCase approvedLoansReportUseCase;

    private Loan loanIn(State state) {
        return Loan.builder().id(7L).amount(BigDecimal.valueOf(30000000)).state(state).build();
    }

    @Test
    @DisplayName("Una aprobación entra al reporte")
    void anApprovalIsAdded() {
        Loan approved = loanIn(State.APPROVED);
        when(approvedLoansReportRepository.add(approved)).thenReturn(Mono.empty());

        StepVerifier.create(approvedLoansReportUseCase.record(approved)).verifyComplete();

        verify(approvedLoansReportRepository).add(approved);
    }

    @Test
    @DisplayName("Un rechazo no cambia el reporte")
    void aRejectionIsIgnored() {
        StepVerifier.create(approvedLoansReportUseCase.record(loanIn(State.REJECTED))).verifyComplete();

        verify(approvedLoansReportRepository, never()).add(any());
    }

    @Test
    @DisplayName("El reporte se lee tal como está guardado")
    void theReportIsReadAsStored() {
        ApprovedLoansReport stored = new ApprovedLoansReport(2, BigDecimal.valueOf(50000000));
        when(approvedLoansReportRepository.get()).thenReturn(Mono.just(stored));

        StepVerifier.create(approvedLoansReportUseCase.get()).expectNext(stored).verifyComplete();
    }
}
