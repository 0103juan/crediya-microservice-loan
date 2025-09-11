package co.com.pragma.model.loandetail;

import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loantype.LoanType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDetail {
    private Loan loan;
    private AuthUser user;
    private LoanType loanType;
}
