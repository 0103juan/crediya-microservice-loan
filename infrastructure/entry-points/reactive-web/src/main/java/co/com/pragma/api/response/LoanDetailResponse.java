package co.com.pragma.api.response;

import co.com.pragma.model.state.State;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
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