package co.com.pragma.api.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtProvider jwtProvider;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        if (jwtProvider.validateToken(token)) {
            Claims claims = jwtProvider.getClaims(token);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

            // --- CAMBIO CLAVE AQUÍ ---
            // En lugar de pasar 'null', pasamos el 'token' como credencial.
            return Mono.just(new UsernamePasswordAuthenticationToken(email, token, authorities));
            // -------------------------

        } else {
            return Mono.empty();
        }
    }
}