package co.com.pragma.sqs.sender.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LoanStatusNotification {
    private Long loanId;
    private String userEmail;
    private String status;
    private String statusDescription;
    private BigDecimal amount;
    private Integer term;
}