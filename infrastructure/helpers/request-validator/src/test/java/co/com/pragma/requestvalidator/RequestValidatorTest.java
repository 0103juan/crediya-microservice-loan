package co.com.pragma.requestvalidator;

import co.com.pragma.model.exceptions.LoanValidationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ServerWebInputException;
import reactor.test.StepVerifier;

class RequestValidatorTest {

    private RequestValidator requestValidator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        requestValidator = new RequestValidator(validator);
    }


    @Test
    void validate_whenObjectIsNull_shouldReturnMonoErrorWithServerWebInputException() {

        StepVerifier.create(requestValidator.validate(null))
                .expectError(ServerWebInputException.class)
                .verify();
    }

    @Test
    void validate_whenObjectIsValid_shouldReturnMonoWithObject() {

        ValidTestObject validObject = new ValidTestObject("test");


        StepVerifier.create(requestValidator.validate(validObject))
                .expectNext(validObject)
                .verifyComplete();
    }

    @Test
    void validate_whenObjectIsInvalid_shouldReturnMonoError() {

        ValidTestObject invalidObject = new ValidTestObject(null);


        StepVerifier.create(requestValidator.validate(invalidObject))
                .expectError(LoanValidationException.class)
                .verify();
    }
}

@Getter
@AllArgsConstructor
class ValidTestObject {

    @NotBlank(message = "El campo no puede estar vacío.")
    private String field;
}