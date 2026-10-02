package co.com.pragma.api.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CustomStatus {

    LOANS_FOUND_SUCCESSFULLY(HttpStatus.OK, "LOANS_FOUND_200", "Solicitudes de préstamo encontradas exitosamente."),
    LOAN_REQUEST_SUCCESSFULLY(HttpStatus.CREATED, "LOAN_REQUEST_SUCCESSFULLY", "Solicitud de préstamo registrada exitosamente."),
    LOAN_STATUS_UPDATED_SUCCESSFULLY(HttpStatus.OK, "LOAN_STATUS_UPDATED_SUCCESSFULLY", "Estado de la solicitud de préstamo actualizado exitosamente."),

    LOAN_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN_NOT_FOUND", "La solicitud de préstamo con el ID '%s' no fue encontrada."),
    LOAN_STATE_CONFLICT(HttpStatus.CONFLICT, "LOAN_STATE_CONFLICT", "La solicitud de préstamo '%s' está en estado %s y no puede pasar a %s."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "La solicitud no se pudo leer: revisa el cuerpo y los parámetros."),
    LOAN_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "LOAN_VALIDATION_ERROR", "La solicitud de préstamo tiene errores de validación."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "El usuario especificado no fue encontrado."),
    INVALID_LOAN_TYPE_ID(HttpStatus.NOT_FOUND, "INVALID_LOAN_TYPE_ID", "El tipo de préstamo con ID '%s' no es válido."),
    INVALID_STATE(HttpStatus.BAD_REQUEST, "INVALID_STATE", "El estado '%s' no es válido."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Ocurrió un error inesperado en el servidor.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}