package co.com.pragma.kafka.producer;

import co.com.pragma.kafka.producer.config.KafkaProducerProperties;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.state.State;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaLoanEventPublisherTest {

    private static final String TOPIC = "loan-decisions";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaLoanEventPublisher publisher;
    private Loan approvedLoan;

    @BeforeEach
    void setUp() {
        publisher = new KafkaLoanEventPublisher(new KafkaProducerProperties(TOPIC), kafkaTemplate, objectMapper);
        approvedLoan = Loan.builder()
                .id(7L)
                .amount(BigDecimal.valueOf(30000000))
                .term(48)
                .userEmail("cliente@test.com")
                .state(State.APPROVED)
                .build();
    }

    @Test
    void publishesTheDecisionToTheTopicKeyedByTheLoanId() throws Exception {
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(TOPIC, 1), 0, 0, 0, 0, 0);
        when(kafkaTemplate.send(eq(TOPIC), eq("7"), anyString())).thenReturn(CompletableFuture.completedFuture(
                new SendResult<>(new ProducerRecord<>(TOPIC, "7", "{}"), metadata)));

        StepVerifier.create(publisher.publishDecision(approvedLoan)).verifyComplete();

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("7"), sent.capture());
        JsonNode event = objectMapper.readTree(sent.getValue());
        assertThat(event.get("loanId").asLong()).isEqualTo(7L);
        assertThat(event.get("userEmail").asText()).isEqualTo("cliente@test.com");
        assertThat(event.get("status").asText()).isEqualTo("APPROVED");
        assertThat(event.get("amount").decimalValue()).isEqualByComparingTo("30000000");
        assertThat(event.get("term").asInt()).isEqualTo(48);
    }

    @Test
    void whenTheBrokerDoesNotConfirm_theErrorReachesTheCaller() {
        when(kafkaTemplate.send(eq(TOPIC), eq("7"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("broker did not answer")));

        StepVerifier.create(publisher.publishDecision(approvedLoan))
                .expectError(TimeoutException.class)
                .verify();
    }
}
