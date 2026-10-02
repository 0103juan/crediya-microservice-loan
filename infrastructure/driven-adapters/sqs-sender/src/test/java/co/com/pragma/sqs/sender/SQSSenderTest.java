package co.com.pragma.sqs.sender;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.state.State;
import co.com.pragma.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQSSenderTest {

    private static final String QUEUE_URL = "http://localhost:4566/000000000000/notificaciones-prestamo";

    @Mock
    private SqsAsyncClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SQSSender sender;
    private Loan approvedLoan;

    @BeforeEach
    void setUp() {
        sender = new SQSSender(new SQSSenderProperties("us-east-1", QUEUE_URL, null), client, objectMapper);
        approvedLoan = Loan.builder()
                .id(7L)
                .amount(BigDecimal.valueOf(30000000))
                .term(48)
                .userEmail("cliente@test.com")
                .state(State.APPROVED)
                .build();
    }

    @Test
    void sendsTheDecisionToTheQueueAddressedToTheClient() throws Exception {
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().messageId("m-1").build()));

        StepVerifier.create(sender.sendLoanStatusNotification(approvedLoan)).verifyComplete();

        ArgumentCaptor<SendMessageRequest> sent = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(sent.capture());
        assertThat(sent.getValue().queueUrl()).isEqualTo(QUEUE_URL);

        JsonNode body = objectMapper.readTree(sent.getValue().messageBody());
        assertThat(body.get("loanId").asLong()).isEqualTo(7L);
        assertThat(body.get("userEmail").asText()).isEqualTo("cliente@test.com");
        assertThat(body.get("status").asText()).isEqualTo("APPROVED");
        assertThat(body.get("amount").decimalValue()).isEqualByComparingTo("30000000");
        assertThat(body.get("term").asInt()).isEqualTo(48);
    }

    @Test
    void whenTheQueueRejectsTheMessage_theErrorReachesTheCaller() {
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(SqsException.builder().message("queue does not exist").build()));

        StepVerifier.create(sender.sendLoanStatusNotification(approvedLoan))
                .expectError(SqsException.class)
                .verify();
    }
}
