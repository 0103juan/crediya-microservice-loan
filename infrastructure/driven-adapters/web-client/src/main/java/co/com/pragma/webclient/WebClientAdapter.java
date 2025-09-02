package co.com.pragma.webclient;

import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.authuser.gateways.AuthUserRepository;
import co.com.pragma.webclient.dto.AuthApiResponse;
import co.com.pragma.webclient.dto.AuthUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

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
                .doOnSuccess(user -> log.info("Usuario encontrado y mapeado desde el servicio auth: {}", user.getEmail()))
                .onErrorResume(WebClientResponseException.class, ex ->
                        ex.getStatusCode() == HttpStatus.NOT_FOUND ? Mono.empty() : Mono.error(ex))
                .doOnError(error -> log.error("Error al consultar el servicio auth: {}", error.getMessage()));
    }

    @Override
    public Mono<AuthUser> findByEmail(String email) { // Asumo que ya renombraste el método y la interfaz
        log.info("Consultando servicio de autenticación para el email: {}", email);

        // Obtenemos el token del contexto de seguridad actual
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
                .doOnSuccess(user -> log.info("Usuario encontrado y mapeado desde el servicio auth: {}", user.getEmail()))
                .doOnError(error -> log.error("Error al consultar el servicio auth: {}", error.getMessage()));
    }
}