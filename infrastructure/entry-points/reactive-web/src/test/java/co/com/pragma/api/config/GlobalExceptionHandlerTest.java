package co.com.pragma.api.config;

import co.com.pragma.api.response.ErrorResponse;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.exceptions.LoanValidationException;
import co.com.pragma.model.exceptions.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleUserNotFoundException() {
        UserNotFoundException ex = new UserNotFoundException("Usuario no encontrado.");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/loans").build());
        Mono<ResponseEntity<ErrorResponse>> response = exceptionHandler.handleUserNotFound(ex, exchange);

        StepVerifier.create(response)
                .assertNext(entity -> {
                    assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
                    assertNotNull(entity.getBody());
                    assertEquals("Usuario no encontrado.", entity.getBody().getMessage());
                })
                .verifyComplete();
    }

    @Test
    void handleInvalidLoanTypeException() {
        InvalidLoanTypeException ex = new InvalidLoanTypeException("Tipo de préstamo no válido.");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/loans").build());
        Mono<ResponseEntity<ErrorResponse>> response = exceptionHandler.handleLoanTypeNotFound(ex, exchange);

        StepVerifier.create(response)
                .assertNext(entity -> {
                    assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
                    assertNotNull(entity.getBody());
                    assertEquals("Tipo de préstamo no válido.", entity.getBody().getMessage());
                })
                .verifyComplete();
    }

    @Test
    void handleLoanValidationException() {
        Map<String, String> errors = Collections.singletonMap("amount", "El monto es inválido");
        LoanValidationException ex = new LoanValidationException("Error de validación", errors);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/loans").build());
        Mono<ResponseEntity<ErrorResponse>> response = exceptionHandler.handleValidationException(ex, exchange);

        StepVerifier.create(response)
                .assertNext(entity -> {
                    assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode());
                    assertNotNull(entity.getBody());
                    assertEquals("Error de validación", entity.getBody().getMessage());
                    assertEquals(errors, entity.getBody().getDetails());
                })
                .verifyComplete();
    }
}