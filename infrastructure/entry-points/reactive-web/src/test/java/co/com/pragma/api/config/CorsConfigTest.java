package co.com.pragma.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@SpringBootTest(classes = CorsConfigTest.TestApplication.class)
@AutoConfigureWebTestClient
@TestPropertySource(properties = "cors.allowed-origins=http://test.com")
class CorsConfigTest {

    /**
     * Reemplazamos @SpringBootApplication por sus componentes para desactivar el escaneo
     * de paquetes y tener control total sobre el contexto.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(CorsConfig.class)
    static class TestApplication {
        @Bean
        public RouterFunction<ServerResponse> testRouter() {
            return RouterFunctions.route()
                    .GET("/test-cors", request -> ServerResponse.ok().build())
                    .build();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldForbidCorsFromInvalidOrigin() {
        webTestClient
                .options().uri("/test-cors")
                .header("Origin", "http://invalid-origin.com")
                .header("Access-control-request-method", "GET")
                .exchange()
                .expectStatus().isForbidden();
    }
}