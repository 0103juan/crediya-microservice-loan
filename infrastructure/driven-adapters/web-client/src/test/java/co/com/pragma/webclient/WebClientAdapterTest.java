package co.com.pragma.webclient;


import co.com.pragma.model.authuser.AuthUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

class WebClientAdapterTest {

    private MockWebServer mockWebServer;
    private WebClientAdapter webClientAdapter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        webClientAdapter = new WebClientAdapter(WebClient.builder(), baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void findByIdNumber_whenUserExists_shouldReturnAuthUser() throws JsonProcessingException {
        AuthUser authUser = new AuthUser("Test", "User", "test@example.com", 123456789L, "Usuario de prueba");
        String jsonResponse = objectMapper.writeValueAsString(authUser);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(webClientAdapter.findByIdNumber("123456789"))
                .expectNextMatches(user -> user.getEmail().equals("test@example.com"))
                .verifyComplete();
    }

    @Test
    void findByIdNumber_whenUserNotFound_shouldReturnEmpty() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(webClientAdapter.findByIdNumber("123456789"))
                .verifyComplete();
    }

    @Test
    void findByIdNumber_whenServiceError_shouldReturnError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(webClientAdapter.findByIdNumber("123456789"))
                .expectError()
                .verify();
    }
}