package co.com.pragma.api.model;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
* PrestamoRequest
*/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoRequest {
    private Double monto = null;
    private Integer plazo = null;
    /**
    * Gets or Sets tipoPrestamo
    */
    @AllArgsConstructor
    public enum TipoPrestamoEnum {
        PERSONAL("PERSONAL"),
        VEHICULO("VEHICULO"),
        VIVIENDA("VIVIENDA");
    
        private final String value;
    
        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
    private TipoPrestamoEnum tipoPrestamo = null;
}