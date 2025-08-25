package co.com.pragma.api.model;

import java.util.Objects;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
* SolicitudResponse
*/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudResponse {
    private UUID idSolicitud = null;
    private String estado = null;
    private OffsetDateTime fechaCreacion = null;
}