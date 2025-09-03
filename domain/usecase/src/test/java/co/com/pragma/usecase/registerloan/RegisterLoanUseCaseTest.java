package co.com.pragma.usecase.registerloan;

import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.authuser.gateways.AuthUserRepository;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
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
    private final Integer validLoanTypeId = 1;

    @BeforeEach
    void setUp() {
        loan = Loan.builder()
                .amount(BigDecimal.valueOf(10000))
                .term(12)
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
        when(authRepository.findByEmail(loan.getUserEmail())).thenReturn(Mono.just(authUser));
        when(loanTypeRepository.getLoanTypeById(validLoanTypeId)).thenReturn(Mono.just(loanType));
        when(loanRepository.saveLoan(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setState(State.REVIEW_PENDING);
            return Mono.just(savedLoan);
        });

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, validLoanTypeId))
                .expectNextMatches(savedLoan ->
                        savedLoan.getUserIdNumber().equals("123456789") &&
                                savedLoan.getState() == State.REVIEW_PENDING &&
                                savedLoan.getLoanType().equals(validLoanTypeId)
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Error al registrar solicitud si el usuario no existe")
    void saveLoan_whenUserNotFound_shouldReturnError() {
        when(authRepository.findByEmail(loan.getUserEmail())).thenReturn(Mono.empty());

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, validLoanTypeId))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Error al registrar si el tipo de préstamo no es válido")
    void saveLoan_whenInvalidLoanType_shouldReturnError() {
        when(authRepository.findByEmail(loan.getUserEmail())).thenReturn(Mono.just(authUser));
        Integer invalidLoanTypeId = 99;
        when(loanTypeRepository.getLoanTypeById(invalidLoanTypeId)).thenReturn(Mono.empty());

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, invalidLoanTypeId))
                .expectError(InvalidLoanTypeException.class)
                .verify();
    }
}