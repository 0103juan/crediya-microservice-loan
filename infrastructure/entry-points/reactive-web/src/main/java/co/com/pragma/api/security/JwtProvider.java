package co.com.pragma.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Obtiene la clave de firma a partir del secreto configurado.
     * Es crucial que esta clave sea la misma que la del microservicio de autenticación.
     * @return La clave para validar la firma del token.
     */
    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Extrae todos los claims (cuerpo) de un token JWT.
     * @param token El token JWT.
     * @return Un objeto Claims que contiene la información del token.
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token).getBody();
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
            Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("Error al validar el token JWT: {}", e.getMessage());
            return false;
        }
    }
}