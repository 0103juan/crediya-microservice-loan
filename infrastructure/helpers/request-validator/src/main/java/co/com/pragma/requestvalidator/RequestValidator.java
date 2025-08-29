package co.com.pragma.requestvalidator;

import co.com.pragma.model.exceptions.LoanValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RequestValidator {

    private final Validator validator;

    public <T> Mono<T> validate(T obj) {
        if (obj == null) {
            return Mono.error(new ServerWebInputException("El cuerpo de la solicitud no puede ser nulo."));
        }

        Set<ConstraintViolation<T>> violations = validator.validate(obj);

        if (violations.isEmpty()) {
            return Mono.just(obj);
        }

        Map<String, List<String>> errorMap = violations.stream()
                .collect(Collectors.groupingBy(
                        violation -> violation.getPropertyPath().toString(),
                        Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())
                ));

        return Mono.error(new LoanValidationException("Error de validación de la solicitud.", errorMap));
    }
}