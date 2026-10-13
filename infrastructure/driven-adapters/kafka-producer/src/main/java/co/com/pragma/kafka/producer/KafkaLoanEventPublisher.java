package co.com.pragma.kafka.producer;

import co.com.pragma.kafka.producer.config.KafkaProducerProperties;
import co.com.pragma.kafka.producer.dto.LoanDecisionEvent;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanEventGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Log4j2
@RequiredArgsConstructor
public class KafkaLoanEventPublisher implements LoanEventGateway {
    private final KafkaProducerProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> publishDecision(Loan loan) {
        // La clave es el id del préstamo: todos sus eventos van a la misma partición y conservan el orden.
        String key = String.valueOf(loan.getId());

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(LoanDecisionEvent.from(loan)))
                .flatMap(event -> Mono.fromFuture(() -> kafkaTemplate.send(properties.decisionsTopic(), key, event)))
                // send() puede bloquear mientras pide los metadatos del tema, así que no corre en el hilo de Netty.
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(result -> log.info("Decisión del préstamo {} publicada en {}-{} con offset {}", key,
                        result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()))
                .doOnError(e -> log.error("No se pudo publicar la decisión del préstamo {}.", key, e))
                .then();
    }
}
