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
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;


import java.util.List;
import java.util.Map;

@Log4j2
@AllArgsConstructor
@Component
public class SolicitudesApiHandler {
//    private final UseCase someUseCase;

    public Mono<ServerResponse> registrarSolicitudPrestamo(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CrearSolicitudRequest.class)
                .flatMap(body -> registrarSolicitudPrestamoMock()) // TODO: Call real use case here -> someUseCase.some()
                .flatMap(response -> ServerResponse.ok().bodyValue(response)); // TODO: Customize response here
    }

    private Mono<SolicitudResponse> registrarSolicitudPrestamoMock() { // TODO: Remove this mock method
        return Mono.fromSupplier(() -> {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            try {
                return mapper.readValue("{\r\n  \"estado\" : \"PENDIENTE_REVISION\",\r\n  \"fechaCreacion\" : \"2025-08-23T16:30:00Z\",\r\n  \"idSolicitud\" : \"550e8400-e29b-41d4-a716-446655440000\"\r\n}", SolicitudResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Cannot parse example to SolicitudResponse");
            }
        });
    }
}
