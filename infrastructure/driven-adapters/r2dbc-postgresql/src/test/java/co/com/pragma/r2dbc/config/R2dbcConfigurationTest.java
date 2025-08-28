package co.com.pragma.r2dbc.config;

import io.r2dbc.pool.ConnectionPool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {
        PostgreSQLConnectionPool.class,
        R2dbcConfig.class
})
@EnableConfigurationProperties(PostgresqlConnectionProperties.class)
class R2dbcConfigurationTest {

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("adapters.r2dbc.host", () -> "localhost");
        registry.add("adapters.r2dbc.port", () -> "5432");
        registry.add("adapters.r2dbc.database", () -> "testdb");
        registry.add("adapters.r2dbc.username", () -> "user");
        registry.add("adapters.r2dbc.password", () -> "pass");
        registry.add("adapters.r2dbc.schema", () -> "public");
    }

    @Autowired
    private ConnectionPool connectionPool;

    @Autowired
    private R2dbcCustomConversions r2dbcCustomConversions;

    @Test
    void shouldLoadConnectionPool() {
        assertNotNull(connectionPool);
    }

    @Test
    void shouldLoadR2dbcCustomConversions() {
        // La prueba ahora simplemente verifica que el bean de conversiones
        // se cargue correctamente en el contexto de Spring.
        // Si la configuración es válida, este bean no será nulo.
        assertNotNull(r2dbcCustomConversions);
    }
}