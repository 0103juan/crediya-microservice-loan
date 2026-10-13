package co.com.pragma.kafka.consumer;

import co.com.pragma.kafka.consumer.dto.LoanDecisionEvent;
import co.com.pragma.model.loan.gateways.NotificationGateway;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Log4j2
@RequiredArgsConstructor
public class LoanDecisionListener {
    private static final Duration NOTIFICATION_TIMEOUT = Duration.ofSeconds(10);

    private final NotificationGateway notificationGateway;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${adapters.kafka.decisions-topic}")
    public void onDecision(String message) throws JsonProcessingException {
        LoanDecisionEvent event = objectMapper.readValue(message, LoanDecisionEvent.class);
        log.info("Decisión recibida para el préstamo {}: {}", event.loanId(), event.status());

        // El hilo del consumidor no es de Reactor, así que puede esperar: el offset solo avanza
        // cuando la notificación quedó encolada, y si falla el mensaje se vuelve a entregar.
        notificationGateway.sendLoanStatusNotification(event.toLoan()).block(NOTIFICATION_TIMEOUT);
    }
}
