package co.com.pragma.api.config;

import co.com.pragma.api.LoanApiRouter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest
@ContextConfiguration(classes = {
        CorsConfig.class,
        SecurityConfig.class,
        SecurityHeadersConfig.class
})
@TestPropertySource(properties = "cors.allowed-origins=http://test.com")
class CorsConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldForbidCorsFromInvalidOrigin() {
        webTestClient.post().uri("/api/v1/loans")
                .header("Origin", "http://invalid-origin.com")
                .exchange()
                .expectStatus().isForbidden();
    }
}