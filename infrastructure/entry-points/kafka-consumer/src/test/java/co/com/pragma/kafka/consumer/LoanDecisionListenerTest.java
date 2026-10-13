package co.com.pragma.kafka.consumer;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.NotificationGateway;
import co.com.pragma.model.state.State;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class LoanDecisionListenerTest {

    @Mock
    private NotificationGateway notificationGateway;

    private LoanDecisionListener listener;

    @BeforeEach
    void setUp() {
        listener = new LoanDecisionListener(notificationGateway, new ObjectMapper());
    }

    @Test
    void aDecisionIsHandedToTheNotificationGateway() throws Exception {
        when(notificationGateway.sendLoanStatusNotification(any(Loan.class))).thenReturn(Mono.empty());

        listener.onDecision("""
                {"loanId":7,"userEmail":"cliente@test.com","status":"REJECTED","amount":30000000,"term":48}""");

        ArgumentCaptor<Loan> notified = ArgumentCaptor.forClass(Loan.class);
        verify(notificationGateway).sendLoanStatusNotification(notified.capture());
        assertThat(notified.getValue().getId()).isEqualTo(7L);
        assertThat(notified.getValue().getUserEmail()).isEqualTo("cliente@test.com");
        assertThat(notified.getValue().getState()).isEqualTo(State.REJECTED);
        assertThat(notified.getValue().getAmount()).isEqualByComparingTo("30000000");
        assertThat(notified.getValue().getTerm()).isEqualTo(48);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not json",
            "{\"loanId\":7,\"userEmail\":\"cliente@test.com\",\"status\":\"SHIPPED\"}",
            "{\"loanId\":7,\"status\":\"APPROVED\"}"
    })
    void aMessageThatIsNotADecisionIsRejectedAsUnreadable(String message) {
        assertThatThrownBy(() -> listener.onDecision(message)).isInstanceOf(JsonProcessingException.class);

        verify(notificationGateway, never()).sendLoanStatusNotification(any());
    }

    @Test
    void whenTheNotificationFails_theErrorReachesTheContainerSoTheMessageIsRetried() {
        when(notificationGateway.sendLoanStatusNotification(any(Loan.class)))
                .thenReturn(Mono.error(new IllegalStateException("cola caída")));

        assertThatThrownBy(() -> listener.onDecision("""
                {"loanId":7,"userEmail":"cliente@test.com","status":"APPROVED","amount":30000000,"term":48}"""))
                .isInstanceOf(IllegalStateException.class);
    }
}
