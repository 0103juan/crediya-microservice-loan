package co.com.pragma.kafka.consumer.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {
    private static final String DEAD_LETTER_SUFFIX = "-dlt";

    // Tres intentos en total, como la cola SQS. Lo que sigue fallando pasa al tema de mensajes muertos
    // y el consumidor continúa con el siguiente, en vez de quedarse atascado en la partición.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (failed, e) -> new TopicPartition(failed.topic() + DEAD_LETTER_SUFFIX, -1));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
        // Un mensaje ilegible no mejora al reintentarlo.
        errorHandler.addNotRetryableExceptions(JsonProcessingException.class);
        return errorHandler;
    }

    @Bean
    public NewTopic loanDecisionsDeadLetterTopic(@Value("${adapters.kafka.decisions-topic}") String decisionsTopic) {
        return TopicBuilder.name(decisionsTopic + DEAD_LETTER_SUFFIX).partitions(1).replicas(1).build();
    }
}
