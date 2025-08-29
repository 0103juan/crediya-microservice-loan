package co.com.pragma.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private final AuthenticationManager authenticationManager;
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        // No es necesario implementar el guardado en un entorno sin estado (stateless)
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            String authToken = authHeader.substring(TOKEN_PREFIX.length());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(authToken, authToken);

            return this.authenticationManager.authenticate(auth)
                    .map(SecurityContextImpl::new);
        }

        return Mono.empty();
    }
}