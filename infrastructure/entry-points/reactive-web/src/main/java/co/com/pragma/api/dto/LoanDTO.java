package co.com.pragma.api.dto;

import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.state.State;
import lombok.*;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanDTO {
    private BigDecimal amount;
    private Integer term;
    private String userIdNumber;
    private String userEmail;
    private LoanType loanType;
    private State state;
}
