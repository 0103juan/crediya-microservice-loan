package co.com.pragma.model.authuser.gateways;

import co.com.pragma.model.authuser.AuthUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AuthUserRepository {
    Mono<AuthUser> findByIdNumber(String idNumber);

    Mono<AuthUser> findByEmail(String userEmail);

    Flux<AuthUser> findAllByEmails(List<String> userEmails);
}
