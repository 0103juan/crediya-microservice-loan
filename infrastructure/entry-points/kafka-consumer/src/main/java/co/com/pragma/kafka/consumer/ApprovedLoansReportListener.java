package co.com.pragma.kafka.consumer;

import co.com.pragma.kafka.consumer.dto.LoanDecisionEvent;
import co.com.pragma.usecase.approvedloansreport.ApprovedLoansReportUseCase;
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
public class ApprovedLoansReportListener {
    private static final Duration REPORT_TIMEOUT = Duration.ofSeconds(10);

    private final ApprovedLoansReportUseCase approvedLoansReportUseCase;
    private final ObjectMapper objectMapper;

    // Otro grupo de consumidores: lee el mismo tema con su propio offset, sin depender del notificador.
    @KafkaListener(topics = "${adapters.kafka.decisions-topic}", groupId = "loan-report")
    public void onDecision(String message) throws JsonProcessingException {
        LoanDecisionEvent event = objectMapper.readValue(message, LoanDecisionEvent.class);
        log.info("Decisión recibida para el reporte, préstamo {}: {}", event.loanId(), event.status());

        approvedLoansReportUseCase.record(event.toLoan()).block(REPORT_TIMEOUT);
    }
}
