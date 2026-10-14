package co.com.pragma.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

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
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        this.parser = Jwts.parserBuilder().setSigningKey(key).build();
        // La primera validación inicializa el generador aleatorio y los lectores JSON de la librería, y en Linux
        // eso lee del disco. Se hace aquí, al arrancar, para que no le toque a la primera petición en el hilo de Netty.
        parser.parseClaimsJws(Jwts.builder().setSubject("warm-up").signWith(key).compact());
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
