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
                            request -> Mono.error(new UserNotFoundException("Usuario no encontrado.")))
                    .andRoute(RequestPredicates.GET("/test-invalid-loan-type"),
                            request -> Mono.error(new InvalidLoanTypeException("Tipo de préstamo inválido.")))
                    .andRoute(RequestPredicates.GET("/test-loan-validation"),
                            request -> {
                                Map<String, List<String>> errors = Collections.singletonMap("campo", List.of("mensaje"));
                                return Mono.error(new LoanValidationException("Error de validación.", errors));
                            })
                    .andRoute(RequestPredicates.GET("/test-generic-exception"),
                            request -> Mono.error(new RuntimeException("Error inesperado.")));
        }
    }

    private void testExceptionHandler(String uri, CustomStatus expectedCustomStatus) {
        webTestClient.get().uri(uri)
                .exchange()
                .expectStatus().isEqualTo(expectedCustomStatus.getHttpStatus())
                .expectBody(new ParameterizedTypeReference<ApiResponse<Object>>() {})
                .value(apiResponse -> {
                    assertEquals(expectedCustomStatus.getCode(), apiResponse.getCode());
                    assertNotNull(apiResponse.getMessage());
                });
    }

    @Test
    void handleUserNotFoundException() {
        testExceptionHandler("/test-user-not-found", CustomStatus.USER_NOT_FOUND);
    }

    @Test
    void handleInvalidLoanTypeException() {
        testExceptionHandler("/test-invalid-loan-type", CustomStatus.INVALID_LOAN_TYPE);
    }

    @Test
    void handleLoanValidationException() {
        webTestClient.get().uri("/test-loan-validation")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Object>>() {})
                .value(apiResponse -> {
                    assertEquals(CustomStatus.LOAN_VALIDATION_ERROR.getCode(), apiResponse.getCode());
                    assertEquals("Error de validación.", apiResponse.getMessage());
                    assertNotNull(apiResponse.getErrors());
                    assertEquals(List.of("mensaje"), apiResponse.getErrors().get("campo"));
                });
    }

    @Test
    void handleGenericException() {
        testExceptionHandler("/test-generic-exception", CustomStatus.INTERNAL_SERVER_ERROR);
    }
}