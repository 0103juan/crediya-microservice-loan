package co.com.pragma.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtProvider {

    // El parser se construye una sola vez: crearlo busca implementaciones en el classpath, y eso lee del disco.
    // Hacerlo en cada petición bloqueaba el hilo de Netty.
    private final JwtParser parser;

    /**
     * @param secret La clave de firma. Es crucial que sea la misma que la del microservicio de autenticación.
     */
    public JwtProvider(@Value("${jwt.secret}") String secret) {
        this.parser = Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(secret.getBytes())).build();
    }

    /**
     * Extrae todos los claims (cuerpo) de un token JWT.
     * @param token El token JWT.
     * @return Un objeto Claims que contiene la información del token.
     */
    public Claims getClaims(String token) {
        return parser.parseClaimsJws(token).getBody();
    }

    /**
     * Extrae el email del sujeto (subject) del token.
     * @param token El token JWT.
     * @return El email del usuario.
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Valida si un token es auténtico y no ha expirado.
     * @param token El token JWT a validar.
     * @return true si el token es válido, false en caso contrario.
     */
    public boolean validateToken(String token) {
        try {
            parser.parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("Error al validar el token JWT: {}", e.getMessage());
            return false;
        }
    }
}
