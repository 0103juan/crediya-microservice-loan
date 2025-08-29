package co.com.pragma.r2dbc.entity;

import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.state.State;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.math.BigInteger;

@Table("loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BigInteger id;
    private BigDecimal amount;
    private Integer term;
    private String userEmail;
    private String userIdNumber;

    @Column("id_loan_type")
    private Integer loanType;

    @Column("id_state")
    private State state;
}

