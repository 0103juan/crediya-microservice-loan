package co.com.pragma.model.loan;

import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.loantype.LoanType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDetail {
    private Loan loan;
    private AuthUser user; // Objeto del microservicio de autenticación
    private LoanType loanType;
}
