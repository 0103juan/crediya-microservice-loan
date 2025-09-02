package co.com.pragma.api.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Datos para registrar la solicitud de un nuevo préstamo en el sistema.
 * Los campos se basan en la información del usuario y las características del préstamo.
 */
@Data
public class RegisterLoanRequest {

    @NotNull(message = "El monto del préstamo no puede ser nulo.")
    @DecimalMin(value = "1.0", message = "El monto del préstamo debe ser mayor que cero.")
    @DecimalMax(value = "100000000.0", message = "El monto del préstamo no puede exceder 100,000,000.")
    private BigDecimal amount;

    @NotNull(message = "El plazo no puede ser nulo.")
    @Positive(message = "El plazo debe ser un número positivo.")
    @Min(value = 1, message = "El plazo mínimo es de 1 mes.")
    @Max(value = 60, message = "El plazo máximo es de 60 meses.")
    private Integer term;


    @NotNull(message = "El ID del tipo de préstamo no puede ser nulo.")
    private Integer loanType;
}