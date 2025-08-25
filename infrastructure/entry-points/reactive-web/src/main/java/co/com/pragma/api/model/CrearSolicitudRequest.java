package co.com.pragma.api.model;

import java.util.Objects;
import co.com.pragma.api.model.ClienteRequest;
import co.com.pragma.api.model.PrestamoRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
* CrearSolicitudRequest
*/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearSolicitudRequest {
    private ClienteRequest cliente = null;
    private PrestamoRequest prestamo = null;
}