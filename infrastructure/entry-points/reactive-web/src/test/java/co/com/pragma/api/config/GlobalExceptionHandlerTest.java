package co.com.pragma.api.config;

import co.com.pragma.api.response.ApiResponse;
import co.com.pragma.api.response.CustomStatus;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.exceptions.LoanValidationException;
import co.com.pragma.model.exceptions.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebFluxTest(excludeAutoConfiguration = {ReactiveSecurityAutoConfiguration.class})
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestRouter.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Configuration
    static class TestRouter {
        @Bean
        public RouterFunction<ServerResponse> testRoutes() {
            return RouterFunctions
                    .route(RequestPredicates.GET("/test-user-not-found"),
                            request -> Mono.error(new UserNotFoundException("usuario.test@pragma.com")))
                    .andRoute(RequestPredicates.GET("/test-invalid-loan-type"),
                            request -> Mono.error(new InvalidLoanTypeException(99)))
                    .andRoute(RequestPredicates.GET("/test-loan-validation"),
                            request -> {
                                Map<String, List<String>> errors = Collections.singletonMap("campo", List.of("mensaje"));
                                return Mono.error(new LoanValidationException(errors));
                            })
                    .andRoute(RequestPredicates.GET("/test-generic-exception"),
                            request -> Mono.error(new RuntimeException("Error inesperado.")));
        }
    }

    private void testExceptionHandler(String uri, CustomStatus expectedCustomStatus, String expectedMessage) {
        webTestClient.get().uri(uri)
                .exchange()
                .expectStatus().isEqualTo(expectedCustomStatus.getHttpStatus())
                .expectBody(new ParameterizedTypeReference<ApiResponse<Object>>() {})
                .value(apiResponse -> {
                    assertEquals(expectedCustomStatus.getCode(), apiResponse.getCode());
                    assertEquals(expectedMessage, apiResponse.getMessage());
                });
    }

    @Test
    void handleUserNotFoundException() {
        CustomStatus status = CustomStatus.USER_NOT_FOUND;
        String expectedMessage = String.format(status.getMessage(), "usuario.test@pragma.com");
        testExceptionHandler("/test-user-not-found", status, expectedMessage);
    }

    @Test
    void handleInvalidLoanTypeException() {
        CustomStatus status = CustomStatus.INVALID_LOAN_TYPE_ID;
        String expectedMessage = String.format(status.getMessage(), 99);
        testExceptionHandler("/test-invalid-loan-type", status, expectedMessage);
    }

    @Test
    void handleLoanValidationException() {
        webTestClient.get().uri("/test-loan-validation")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Object>>() {})
                .value(apiResponse -> {
                    assertEquals(CustomStatus.LOAN_VALIDATION_ERROR.getCode(), apiResponse.getCode());
                    // <-- CAMBIO AQUÍ: Usamos el mensaje del enum en lugar de un string quemado.
                    assertEquals(CustomStatus.LOAN_VALIDATION_ERROR.getMessage(), apiResponse.getMessage());
                    assertNotNull(apiResponse.getErrors());
                    assertEquals(List.of("mensaje"), apiResponse.getErrors().get("campo"));
                });
    }

    @Test
    void handleGenericException() {
        CustomStatus status = CustomStatus.INTERNAL_SERVER_ERROR;
        testExceptionHandler("/test-generic-exception", status, status.getMessage());
    }
}