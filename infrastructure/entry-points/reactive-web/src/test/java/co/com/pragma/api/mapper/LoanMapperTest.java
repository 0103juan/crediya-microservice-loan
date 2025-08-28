package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.LoanDTO;
import co.com.pragma.api.request.RegisterLoanRequest;
import co.com.pragma.api.response.LoanResponse;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.state.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoanMapperTest {

    private final LoanMapper loanMapper = Mappers.getMapper(LoanMapper.class);
    private Loan loan;
    private RegisterLoanRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterLoanRequest();
        request.setAmount(new BigDecimal("15000.00"));
        request.setTerm(24);
        request.setUserEmail("test@example.com");
        request.setUserIdNumber("12345678");
        request.setLoanType(1);

        loan = Loan.builder()
                .amount(new BigDecimal("15000.00"))
                .term(24)
                .userEmail("test@example.com")
                .userIdNumber("12345678")
                .loanType(1)
                .state(State.REVIEW_PENDING)
                .build();
    }

    @Test
    void shouldMapRegisterLoanRequestToLoan() {
        // Act
        Loan mappedLoan = loanMapper.toModel(request);

        // Assert
        assertNotNull(mappedLoan);
        assertEquals(request.getAmount(), mappedLoan.getAmount());
        assertEquals(request.getTerm(), mappedLoan.getTerm());
        assertEquals(request.getUserEmail(), mappedLoan.getUserEmail());
        assertEquals(request.getUserIdNumber(), mappedLoan.getUserIdNumber());
        assertNull(mappedLoan.getState()); // El estado no se mapea desde el request
        assertNull(mappedLoan.getLoanType()); // El tipo de préstamo se maneja por separado
    }

    @Test
    void shouldMapLoanToLoanResponse() {
        // Act
        LoanResponse response = loanMapper.toResponse(loan);

        // Assert
        assertNotNull(response);
        assertEquals(loan.getUserEmail(), response.getUserEmail());
        assertEquals(loan.getUserIdNumber(), response.getUserIdNumber());
        assertEquals(loan.getState(), response.getState());
    }

    @Test
    void shouldMapLoanToLoanDTO() {
        // Act
        LoanDTO dto = loanMapper.toDTO(loan);

        // Assert
        assertNotNull(dto);
        assertEquals(loan.getAmount(), dto.getAmount());
        assertEquals(loan.getUserEmail(), dto.getUserEmail());
        assertEquals(loan.getState(), dto.getState());
    }

    @Test
    void shouldMapLoanListToLoanDTOList() {
        // Arrange
        List<Loan> loanList = List.of(loan);

        // Act
        List<LoanDTO> dtoList = loanMapper.toListDTO(loanList);

        // Assert
        assertNotNull(dtoList);
        assertEquals(1, dtoList.size());
        assertEquals(loan.getAmount(), dtoList.get(0).getAmount());
    }

    @Test
    void shouldThrowInvalidLoanTypeExceptionForInvalidState() {
        // Act & Assert
        InvalidLoanTypeException exception = assertThrows(InvalidLoanTypeException.class, () -> {
            loanMapper.toState("ESTADO_INVALIDO");
        });

        assertEquals("El estado 'ESTADO_INVALIDO' no es válido.", exception.getMessage());
    }

    @Test
    void shouldMapStringToState() {
        // Act
        State state = loanMapper.toState("APPROVED");

        // Assert
        assertEquals(State.APPROVED, state);
    }
}