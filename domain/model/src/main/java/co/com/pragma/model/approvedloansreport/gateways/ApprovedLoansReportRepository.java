package co.com.pragma.model.approvedloansreport.gateways;

import co.com.pragma.model.approvedloansreport.ApprovedLoansReport;
import co.com.pragma.model.loan.Loan;
import reactor.core.publisher.Mono;

public interface ApprovedLoansReportRepository {
    // Sumar dos veces el mismo préstamo no cambia el reporte.
    Mono<Void> add(Loan approvedLoan);

    Mono<ApprovedLoansReport> get();
}
