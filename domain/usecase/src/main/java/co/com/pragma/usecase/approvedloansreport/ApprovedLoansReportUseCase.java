package co.com.pragma.usecase.approvedloansreport;

import co.com.pragma.model.approvedloansreport.ApprovedLoansReport;
import co.com.pragma.model.approvedloansreport.gateways.ApprovedLoansReportRepository;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.state.State;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ApprovedLoansReportUseCase {

    private final ApprovedLoansReportRepository approvedLoansReportRepository;

    // Recibe todas las decisiones; al reporte solo entran las aprobaciones.
    public Mono<Void> record(Loan decidedLoan) {
        if (decidedLoan.getState() != State.APPROVED) {
            return Mono.empty();
        }
        return approvedLoansReportRepository.add(decidedLoan);
    }

    public Mono<ApprovedLoansReport> get() {
        return approvedLoansReportRepository.get();
    }
}
