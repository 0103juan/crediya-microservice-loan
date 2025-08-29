package co.com.pragma.api;

import co.com.pragma.api.mapper.LoanMapper;
import co.com.pragma.api.mapper.LoanMapperImpl;
import co.com.pragma.api.request.RegisterLoanRequest;
import co.com.pragma.model.exceptions.LoanValidationException;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.state.State;
import co.com.pragma.requestvalidator.RequestValidator;
import co.com.pragma.usecase.registerloan.RegisterLoanUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApiHandlerTest {

    @Mock
    private RegisterLoanUseCase registerLoanUseCase;
    @Mock
    private RequestValidator validator;

    @Spy
    private LoanMapper loanMapper = new LoanMapperImpl();

    @InjectMocks
    private LoanApiHandler loanApiHandler;

    private RegisterLoanRequest request;
    private Loan loan;

    @BeforeEach
    void setUp() {
        request = new RegisterLoanRequest();
        request.setAmount(BigDecimal.valueOf(20000));
        request.setTerm(24);
        request.setUserEmail("handler@test.com");
        request.setUserIdNumber("987654321");
        request.setLoanType(2);

        loan = Loan.builder()
                .amount(request.getAmount())
                .term(request.getTerm())
                .userEmail(request.getUserEmail())
                .userIdNumber(request.getUserIdNumber())
                .loanType(request.getLoanType())
                .state(State.REVIEW_PENDING)
                .build();
    }

    @Test
    void listenRegister_Success() {
        when(validator.validate(any(RegisterLoanRequest.class))).thenReturn(Mono.just(request));
        when(registerLoanUseCase.saveLoan(any(Loan.class), any(Integer.class))).thenReturn(Mono.just(loan));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        StepVerifier.create(loanApiHandler.listenRegister(serverRequest))
                .expectNextMatches(response ->
                        response.statusCode().equals(HttpStatus.CREATED)
                )
                .verifyComplete();
    }

    @Test
    void listenRegister_ValidationError() {
        Map<String, String> errors = Collections.singletonMap("amount", "El monto es inválido");
        when(validator.validate(any(RegisterLoanRequest.class)))
                .thenReturn(Mono.error(new LoanValidationException("Error de validación", errors)));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        StepVerifier.create(loanApiHandler.listenRegister(serverRequest))
                .expectError(LoanValidationException.class)
                .verify();
    }

    @Test
    void listenRegister_UseCaseError() {
        when(validator.validate(any(RegisterLoanRequest.class))).thenReturn(Mono.just(request));
        when(registerLoanUseCase.saveLoan(any(Loan.class), any(Integer.class)))
                .thenReturn(Mono.error(new RuntimeException("Error en el caso de uso")));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .body(Mono.just(request));

        StepVerifier.create(loanApiHandler.listenRegister(serverRequest))
                .expectError(RuntimeException.class)
                .verify();
    }
}