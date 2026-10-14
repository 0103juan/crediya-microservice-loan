package co.com.pragma.kafka.consumer;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.state.State;
import co.com.pragma.usecase.approvedloansreport.ApprovedLoansReportUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovedLoansReportListenerTest {

    @Mock
    private ApprovedLoansReportUseCase approvedLoansReportUseCase;

    private ApprovedLoansReportListener listener;

    @BeforeEach
    void setUp() {
        listener = new ApprovedLoansReportListener(approvedLoansReportUseCase, new ObjectMapper());
    }

    @Test
    void aDecisionIsHandedToTheReport() throws Exception {
        when(approvedLoansReportUseCase.record(any(Loan.class))).thenReturn(Mono.empty());

        listener.onDecision("""
                {"loanId":7,"userEmail":"cliente@test.com","status":"APPROVED","amount":30000000,"term":48}""");

        ArgumentCaptor<Loan> recorded = ArgumentCaptor.forClass(Loan.class);
        verify(approvedLoansReportUseCase).record(recorded.capture());
        assertThat(recorded.getValue().getId()).isEqualTo(7L);
        assertThat(recorded.getValue().getState()).isEqualTo(State.APPROVED);
        assertThat(recorded.getValue().getAmount()).isEqualByComparingTo("30000000");
    }

    @Test
    void aMessageThatIsNotADecisionIsRejectedAsUnreadable() {
        assertThatThrownBy(() -> listener.onDecision("not json")).isInstanceOf(JsonProcessingException.class);

        verify(approvedLoansReportUseCase, never()).record(any());
    }
}
