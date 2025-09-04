package co.com.pragma.model.loan;

import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.state.State;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Loan {
    private BigDecimal amount;
    private Integer term;
    private String userIdNumber;
    private String userEmail;
    private LoanType loanType;
    private State state;
}
