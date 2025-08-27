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
    void saveLoan_Success() {
        when(authRepository.findByIdNumber(loan.getUserIdNumber())).thenReturn(Mono.just(authUser));
        when(loanTypeRepository.getLoanTypeById(1)).thenReturn(Mono.just(loanType));
        when(loanRepository.saveLoan(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setState(State.REVIEW_PENDING);
            return Mono.just(savedLoan);
        });

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, 1))
                .expectNextMatches(savedLoan ->
                        savedLoan.getUserIdNumber().equals("123456789") &&
                                savedLoan.getState() == State.REVIEW_PENDING &&
                                savedLoan.getLoanType() == 1
                )
                .verifyComplete();
    }

    @Test
    void saveLoan_UserNotFound() {
        when(authRepository.findByIdNumber(loan.getUserIdNumber())).thenReturn(Mono.empty());

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, 1))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void saveLoan_EmailMismatch() {
        authUser.setEmail("another@email.com");
        when(authRepository.findByIdNumber(loan.getUserIdNumber())).thenReturn(Mono.just(authUser));

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, 1))
                .expectErrorMatches(throwable ->
                        throwable instanceof LoanValidationException &&
                                ((LoanValidationException) throwable).getErrors().containsKey("userEmail")
                )
                .verify();
    }

    @Test
    void saveLoan_InvalidLoanType() {
        when(authRepository.findByIdNumber(loan.getUserIdNumber())).thenReturn(Mono.just(authUser));
        when(loanTypeRepository.getLoanTypeById(99)).thenReturn(Mono.empty());

        StepVerifier.create(registerLoanUseCase.saveLoan(loan, 99))
                .expectError(InvalidLoanTypeException.class)
                .verify();
    }
}