package co.com.pragma.api;

import co.com.pragma.api.model.CrearSolicitudRequest;
import co.com.pragma.api.model.ErrorResponse;
import co.com.pragma.api.model.SolicitudResponse;
import lombok.extern.log4j.Log4j2;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.HEAD;
import static org.springframework.web.reactive.function.server.RequestPredicates.headers;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;


import java.util.List;
import java.util.Map;

@Log4j2
@AllArgsConstructor
@Configuration
public class SolicitudesApiRouter {

    @Bean
    public RouterFunction<ServerResponse> routerFunctionSolicitudesApi(SolicitudesApiHandler handler) {
        return route(POST("/api/v1/solicitud"), handler::registrarSolicitudPrestamo)
;
    }
}
