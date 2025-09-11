package co.com.pragma.webclient;

import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.authuser.gateways.AuthUserRepository;
import co.com.pragma.webclient.dto.AuthApiResponse;
import co.com.pragma.webclient.dto.AuthUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class WebClientAdapter implements AuthUserRepository {

    private final WebClient webClient;

    public WebClientAdapter(WebClient.Builder webClientBuilder, @Value("${adapters.auth.url}") String authUrl) {
        this.webClient = webClientBuilder.baseUrl(authUrl).build();
    }

    @Override
    public Mono<AuthUser> findByIdNumber(String idNumber) {
        log.info("Consultando servicio de autenticación para el idNumber: {}", idNumber);

        ParameterizedTypeReference<AuthApiResponse<AuthUserResponse>> responseType =
                new ParameterizedTypeReference<>() {};

        return webClient.get()
                .uri("/users/{idNumber}", idNumber)
                .retrieve()
                .bodyToMono(responseType)
                .map(AuthApiResponse::getData)
                .map(userResponse -> AuthUser.builder()
                        .firstName(userResponse.getFirstName())
                        .lastName(userResponse.getLastName())
                        .email(userResponse.getEmail())
                        .idNumber(Long.parseLong(userResponse.getIdNumber()))
                        .build())
                .doOnSuccess(user -> log.info("Usuario encontrado y mapeado desde el servicio auth con el idNumber: {}", user.getEmail()))
                .onErrorResume(WebClientResponseException.class, ex ->
                        ex.getStatusCode() == HttpStatus.NOT_FOUND ? Mono.empty() : Mono.error(ex))
                .doOnError(error -> log.error("Error al consultar el servicio auth con el idNumber: {}", error.getMessage()));
    }

    @Override
    public Mono<AuthUser> findByEmail(String email) {
        log.info("Consultando servicio de autenticación para el email: {}", email);

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    String tokenValue = authentication.getCredentials().toString();

                    ParameterizedTypeReference<AuthApiResponse<AuthUserResponse>> responseType = new ParameterizedTypeReference<>() {};

                    return webClient.get()
                            .uri("/users/email/{email}", email)
                            .header("Authorization", "Bearer " + tokenValue)
                            .retrieve()
                            .bodyToMono(responseType)
                            .map(AuthApiResponse::getData)
                            .map(userResponse -> AuthUser.builder()
                                    .firstName(userResponse.getFirstName())
                                    .lastName(userResponse.getLastName())
                                    .email(userResponse.getEmail())
                                    .idNumber(Long.parseLong(userResponse.getIdNumber()))
                                    .build());
                })
                .doOnSuccess(user -> log.info("Usuario encontrado y mapeado desde el servicio auth con email: {}", user.getEmail()))
                .doOnError(error -> log.error("Error al consultar el servicio auth con email: {}", error.getMessage()));
    }

    @Override
    public Flux<AuthUser> findAllByEmails(List<String> userEmails) {
        if (userEmails == null || userEmails.isEmpty()) {
            return Flux.empty();
        }
        log.info("Consultando servicio de autenticación para {} emails.", userEmails.size());

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(authentication -> {
                    String tokenValue = authentication.getCredentials().toString();

                    ParameterizedTypeReference<AuthApiResponse<List<AuthUserResponse>>> responseType =
                            new ParameterizedTypeReference<>() {};

                    Map<String, List<String>> requestBody = Collections.singletonMap("emails", userEmails);

                    return webClient.post()
                            .uri("/users/emails")
                            .header("Authorization", "Bearer " + tokenValue)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(responseType)
                            // --- CAMBIO CLAVE AQUÍ ---
                            .flatMapMany(apiResponse ->
                                    Flux.fromIterable(Optional.ofNullable(apiResponse.getData()).orElse(List.of()))
                            )
                            .map(userResponse -> AuthUser.builder()
                                    .firstName(userResponse.getFirstName())
                                    .lastName(userResponse.getLastName())
                                    .email(userResponse.getEmail())
                                    .idNumber(Long.parseLong(userResponse.getIdNumber()))
                                    .baseSalary(userResponse.getBaseSalary())
                                    .build());
                })
                .doOnComplete(() -> log.info("Usuarios por email recuperados exitosamente."))
                .doOnError(error -> log.error("Error al consultar el servicio auth para múltiples emails: {}", error.getMessage()));
    }
}