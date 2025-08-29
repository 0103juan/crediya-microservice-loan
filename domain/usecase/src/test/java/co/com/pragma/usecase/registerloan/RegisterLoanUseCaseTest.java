package co.com.pragma.usecase.registerloan;

import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.authuser.gateways.AuthUserRepository;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.exceptions.LoanValidationException;
import co.com.pragma.model.exceptions.UserNotFoundException;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.state.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterLoanUseCaseTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private LoanTypeRepository loanTypeRepository;
    @Mock
    private AuthUserRepository authRepository;

    @InjectMocks
    private RegisterLoanUseCase registerLoanUseCase;

    private Loan loan;
    private AuthUser authUser;
    private LoanType loanType;
    private final Integer VALID_LOAN_TYPE_ID = 1;
    private final Integer INVALID_LOAN_TYPE_ID = 99;

    @BeforeEach
    void setUp() {
        loan = Loan.builder()
                .amount(BigDecimal.valueOf(10000))
                .term(12)
                .userIdNumber("123456789")
                .userEmail("test@pragma.com")
                .build();

        authUser = AuthUser.builder()
                .idNumber(123456789L)
                .email("test@pragma.com")
                .firstName("Test")
                .lastName("User")
                .build();

        loanType = LoanType.builder()
                .name("PERSONAL")
                .minimumAmount(BigDecimal.valueOf(1000))
                .maximumAmount(BigDecimal.valueOf(50000))
                .interestRate(BigDecimal.valueOf(0.05))
                .automaticValidation(true)
                .build();
    }

    @Test
    @DisplayName("Registro exitoso de una nueva solicitud de préstamo")
    void saveLoan_Success() {
        when(authRepository.findByIdNumber(loan.getUserIdNumber())).thenReturn(Mono.just(authUser));
        when(loanTypeRepository.getLoanTypeById(VALID_LOAN_TYPE_ID)).thenReturn(Mono.just(loanType));
        when(loanRepository.saveLoan(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setState(State.REVIEW_PENDING); // Simula el estado asignado
            return Mono.just(savedLoan);
        });

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, VALID_LOAN_TYPE_ID))
                .expectNextMatches(savedLoan ->
                        savedLoan.getUserIdNumber().equals("123456789") &&
                                savedLoan.getState() == State.REVIEW_PENDING &&
                                savedLoan.getLoanType().equals(VALID_LOAN_TYPE_ID)
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Error al registrar solicitud si el usuario no existe")
    void saveLoan_whenUserNotFound_shouldReturnError() {
        when(authRepository.findByIdNumber(loan.getUserIdNumber())).thenReturn(Mono.empty());

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, VALID_LOAN_TYPE_ID))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Error al registrar si el email no coincide con el del usuario registrado")
    void saveLoan_whenEmailMismatch_shouldReturnValidationError() {
        authUser.setEmail("another@email.com");
        when(authRepository.findByIdNumber(loan.getUserIdNumber())).thenReturn(Mono.just(authUser));

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, VALID_LOAN_TYPE_ID))
                .expectErrorMatches(throwable ->
                        throwable instanceof LoanValidationException &&
                                ((LoanValidationException) throwable).getErrors().containsKey("userEmail")
                )
                .verify();
    }

    @Test
    @DisplayName("Error al registrar si el tipo de préstamo no es válido")
    void saveLoan_whenInvalidLoanType_shouldReturnError() {
        when(authRepository.findByIdNumber(loan.getUserIdNumber())).thenReturn(Mono.just(authUser));
        when(loanTypeRepository.getLoanTypeById(INVALID_LOAN_TYPE_ID)).thenReturn(Mono.empty());

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, INVALID_LOAN_TYPE_ID))
                .expectError(InvalidLoanTypeException.class)
                .verify();
    }
}