package co.com.pragma.api.mapper;

import co.com.pragma.api.request.RegisterLoanRequest;
import co.com.pragma.api.response.LoanDetailResponse;
import co.com.pragma.api.response.LoanResponse;
import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loandetail.LoanDetail;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.state.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoanMapperTest {

    private final LoanMapper loanMapper = Mappers.getMapper(LoanMapper.class);
    private Loan loan;
    private RegisterLoanRequest request;
    private LoanDetail loanDetail;

    @BeforeEach
    void setUp() {
        request = new RegisterLoanRequest();
        request.setAmount(new BigDecimal("15000.00"));
        request.setTerm(24);
        request.setLoanType(1);

        loan = Loan.builder()
                .amount(new BigDecimal("15000.00"))
                .term(24)
                .userEmail("test@example.com")
                .userIdNumber("12345678")
                .loanTypeId(1)
                .state(State.REVIEW_PENDING)
                .build();

        AuthUser user = AuthUser.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .baseSalary(BigDecimal.valueOf(2000))
                .build();

        LoanType loanType = LoanType.builder()
                .name("PERSONAL")
                .interestRate(BigDecimal.valueOf(10))
                .build();

        loanDetail = new LoanDetail(loan, user, loanType);
    }

    @Test
    void shouldMapRegisterLoanRequestToLoan() {
        Loan mappedLoan = loanMapper.toModel(request);

        assertNotNull(mappedLoan);
        assertEquals(request.getAmount(), mappedLoan.getAmount());
        assertEquals(request.getTerm(), mappedLoan.getTerm());
        assertNull(mappedLoan.getState());
        assertEquals(1, mappedLoan.getLoanTypeId());
    }

    @Test
    void shouldMapLoanToLoanResponse() {
        LoanResponse response = loanMapper.toResponse(loan);

        assertNotNull(response);
        assertEquals(loan.getUserEmail(), response.getUserEmail());
        assertEquals(loan.getUserIdNumber(), response.getUserIdNumber());
        assertEquals(loan.getState(), response.getState());
    }

    @Test
    void shouldMapLoanDetailToLoanDetailResponse() {
        LoanDetailResponse response = loanMapper.toLoanDetailResponse(loanDetail);

        assertNotNull(response);
        assertEquals("Test User", response.getUserName());
        assertEquals("PERSONAL", response.getLoanTypeName());
        assertEquals(0, loan.getAmount().compareTo(response.getAmount()));
        assertTrue(response.getMonthlyPayment().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void shouldReturnZeroMonthlyPaymentWhenRateIsNull() {
        loanDetail.getLoanType().setInterestRate(null);
        BigDecimal monthlyPayment = loanMapper.calculateMonthlyPayment(loanDetail);
        assertEquals(0, BigDecimal.ZERO.compareTo(monthlyPayment));
    }

    @Test
    void shouldCalculateMonthlyPaymentWithZeroInterest() {
        loanDetail.getLoanType().setInterestRate(BigDecimal.ZERO);
        BigDecimal expectedPayment = loan.getAmount().divide(BigDecimal.valueOf(loan.getTerm()), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = loanMapper.calculateMonthlyPayment(loanDetail);
        assertEquals(0, expectedPayment.compareTo(monthlyPayment));
    }

    @Test
    void shouldReturnNullUserNameWhenUserIsNull() {
        loanDetail.setUser(null);
        String userName = loanMapper.toUserName(loanDetail);
        assertNull(userName);
    }

    @Test
    void shouldThrowInvalidLoanTypeExceptionForInvalidState() {
        // Al fallar la conversión de estado, se debe lanzar InvalidLoanTypeException.
        // El mapper no tiene contexto para saber si es un estado o un tipo de préstamo,
        // por lo que reutiliza la excepción.
        var exception = assertThrows(InvalidLoanTypeException.class, () -> {
            loanMapper.toState("ESTADO_INVALIDO");
        });

        // <-- CAMBIO AQUÍ: Verificamos el identificador, no el mensaje.
        assertEquals("El estado 'ESTADO_INVALIDO' no es válido.", exception.getIdentifier());
    }
}