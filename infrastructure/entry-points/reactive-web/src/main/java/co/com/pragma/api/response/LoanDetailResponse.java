package co.com.pragma.api.response;

import co.com.pragma.model.state.State;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor      // <-- Cambio aquí
@AllArgsConstructor
public class LoanDetailResponse {
    private BigDecimal amount;
    private Integer term;
    private String userEmail;
    private String userName;
    private String loanTypeName;
    private BigDecimal interestRate;
    private State state;
    private BigDecimal baseSalary;
    private BigDecimal monthlyPayment;
}