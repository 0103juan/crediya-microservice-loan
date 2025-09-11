package co.com.pragma.api.config;

import co.com.pragma.api.response.ApiResponse;
import co.com.pragma.api.response.CustomStatus;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.exceptions.LoanValidationException;
import co.com.pragma.model.exceptions.UserNotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@Order(-2)
@Log4j2
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalExceptionHandler(ErrorAttributes errorAttributes, ApplicationContext applicationContext,
                                  ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(final ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(final ServerRequest request) {
        Throwable error = getError(request);
        String finalMessage;
        CustomStatus customStatus;
        Map<String, List<String>> errors = null;

        if (error instanceof UserNotFoundException e) {
            customStatus = CustomStatus.USER_NOT_FOUND;
            finalMessage = String.format(customStatus.getMessage(), e.getUserIdentifier());
        } else if (error instanceof InvalidLoanTypeException e) {
            // Distinguimos si el identificador es un ID de tipo de préstamo o un nombre de estado
            if (e.getIdentifier() instanceof Integer) {
                customStatus = CustomStatus.INVALID_LOAN_TYPE_ID;
            } else {
                customStatus = CustomStatus.INVALID_STATE;
            }
            finalMessage = String.format(customStatus.getMessage(), e.getIdentifier());
        } else if (error instanceof LoanValidationException e) {
            customStatus = CustomStatus.LOAN_VALIDATION_ERROR;
            finalMessage = customStatus.getMessage(); // El mensaje es genérico
            errors = e.getErrors();
        } else {
            customStatus = CustomStatus.INTERNAL_SERVER_ERROR;
            finalMessage = customStatus.getMessage();
        }

        log.error("Error manejado: {} - Status: {} - Path: {}", finalMessage, customStatus.getHttpStatus(), request.path(), error);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(customStatus.getCode())
                .message(finalMessage)
                .path(request.path())
                .errors(errors)
                .build();

        return ServerResponse.status(customStatus.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(apiResponse));
    }
}