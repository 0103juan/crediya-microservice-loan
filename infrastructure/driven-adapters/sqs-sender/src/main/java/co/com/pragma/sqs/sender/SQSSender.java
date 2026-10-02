package co.com.pragma.sqs.sender;

import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.NotificationGateway;
import co.com.pragma.sqs.sender.config.SQSSenderProperties;
import co.com.pragma.sqs.sender.dto.LoanStatusNotification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSender implements NotificationGateway {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> sendLoanStatusNotification(Loan loan) {
        log.info("Preparando notificación para el préstamo del usuario {} con estado: {}",
                loan.getUserEmail(), loan.getState());

        return Mono.fromCallable(() -> buildNotification(loan))
                .flatMap(this::convertToJson)
                .flatMap(messageBody -> {
                    SendMessageRequest sendMessageRequest = buildRequest(messageBody);

                    log.info("Enviando mensaje a la cola SQS...");

                    return Mono.fromFuture(client.sendMessage(sendMessageRequest));
                })
                .doOnSuccess(response -> log.info("Mensaje enviado exitosamente a SQS. MessageId: {}", response.messageId()))
                .doOnError(e -> log.error("No se pudo enviar el mensaje a SQS.", e))
                .then();
    }

    private LoanStatusNotification buildNotification(Loan loan) {
        return LoanStatusNotification.builder()
                .loanId(loan.getId())
                .userEmail(loan.getUserEmail())
                .status(loan.getState().name())
                .statusDescription(loan.getState().getDescription())
                .amount(loan.getAmount())
                .term(loan.getTerm())
                .build();
    }

    private Mono<String> convertToJson(LoanStatusNotification notification) {
        try {
            return Mono.just(objectMapper.writeValueAsString(notification));
        } catch (JsonProcessingException e) {
            log.error("Error al serializar la notificación a JSON.", e);
            return Mono.error(e);
        }
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
