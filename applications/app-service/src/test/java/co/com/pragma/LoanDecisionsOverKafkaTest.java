package co.com.pragma;

import co.com.pragma.kafka.consumer.LoanDecisionListener;
import co.com.pragma.kafka.consumer.config.KafkaConsumerConfig;
import co.com.pragma.kafka.producer.KafkaLoanEventPublisher;
import co.com.pragma.kafka.producer.config.KafkaProducerConfig;
import co.com.pragma.kafka.producer.config.KafkaProducerProperties;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanEventGateway;
import co.com.pragma.model.loan.gateways.NotificationGateway;
import co.com.pragma.model.state.State;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The producer and the consumer against a real broker, started in this JVM. Only the SQS side is a mock.
@SpringBootTest(classes = {KafkaAutoConfiguration.class, JacksonAutoConfiguration.class,
        KafkaProducerConfig.class, KafkaLoanEventPublisher.class,
        KafkaConsumerConfig.class, LoanDecisionListener.class})
@EnableConfigurationProperties(KafkaProducerProperties.class)
@EmbeddedKafka(bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class LoanDecisionsOverKafkaTest {

    private static final String TOPIC = "loan-decisions";
    private static final Duration WAIT = Duration.ofSeconds(30);

    @MockitoBean
    private NotificationGateway notificationGateway;
    @Autowired
    private LoanEventGateway loanEventGateway;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void aPublishedDecisionReachesTheNotifierThroughTheTopic() {
        when(notificationGateway.sendLoanStatusNotification(any(Loan.class))).thenReturn(Mono.empty());
        Loan approved = Loan.builder().id(7L).userEmail("cliente@test.com").state(State.APPROVED)
                .amount(BigDecimal.valueOf(30000000)).term(48).build();

        loanEventGateway.publishDecision(approved).block(WAIT);

        ArgumentCaptor<Loan> notified = ArgumentCaptor.forClass(Loan.class);
        verify(notificationGateway, timeout(WAIT.toMillis())).sendLoanStatusNotification(notified.capture());
        assertThat(notified.getValue().getId()).isEqualTo(7L);
        assertThat(notified.getValue().getUserEmail()).isEqualTo("cliente@test.com");
        assertThat(notified.getValue().getState()).isEqualTo(State.APPROVED);
        assertThat(notified.getValue().getAmount()).isEqualByComparingTo("30000000");
    }

    @Test
    void aMessageThatIsNotADecisionEndsInTheDeadLetterTopic() throws Exception {
        kafkaTemplate.send(TOPIC, "8", "not a decision").get();

        try (Consumer<String, String> deadLetters = new DefaultKafkaConsumerFactory<>(
                KafkaTestUtils.consumerProps("dead-letter-check", "true", broker),
                new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            deadLetters.subscribe(List.of(TOPIC + "-dlt"));
            ConsumerRecord<String, String> dead = KafkaTestUtils.getSingleRecord(deadLetters, TOPIC + "-dlt", WAIT);

            assertThat(dead.key()).isEqualTo("8");
            assertThat(dead.value()).isEqualTo("not a decision");
        }
        verify(notificationGateway, never()).sendLoanStatusNotification(any());
    }
}
