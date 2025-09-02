package co.com.pragma.model.authuser.gateways;

import co.com.pragma.model.authuser.AuthUser;
import reactor.core.publisher.Mono;

public interface AuthUserRepository {
    Mono<AuthUser> findByIdNumber(String idNumber);

    Mono<AuthUser> findByEmail(String userEmail);
}
