package co.com.pragma.usecase.findloans;

import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.authuser.gateways.AuthUserRepository;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loandetail.LoanDetail;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.loanquery.LoanQuery;
import co.com.pragma.model.paginatedresult.PaginatedResult;
import co.com.pragma.model.state.State;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class FindLoansUseCase {

    private final LoanRepository loanRepository;
    private final AuthUserRepository authUserRepository;
    private final LoanTypeRepository loanTypeRepository;

    public Mono<PaginatedResult<LoanDetail>> findByStatus(LoanQuery query) {
        List<State> statuses = List.of(State.MANUAL_REVIEW);
        LoanQuery finalQuery = query.toBuilder().states(statuses).build();

        return loanRepository.findByQuery(finalQuery)
                .flatMap(paginatedResult -> {
                    if (paginatedResult.content().isEmpty()) {
                        return Mono.just(new PaginatedResult<>(List.of(), 0, 0, query.getPage(), query.getSize()));
                    }

                    List<Loan> loans = paginatedResult.content();

                    List<String> userEmails = loans.stream().map(Loan::getUserEmail).distinct().toList();
                    List<Integer> loanTypeIds = loans.stream().map(Loan::getLoanTypeId).distinct().toList();

                    Mono<Map<String, AuthUser>> usersMapMono = authUserRepository.findAllByEmails(userEmails)
                            .collectMap(AuthUser::getEmail);

                    Mono<Map<Integer, LoanType>> loanTypesMapMono = loanTypeRepository.findAllByIds(loanTypeIds)
                            .collectMap(LoanType::getId);

                    return Mono.zip(usersMapMono, loanTypesMapMono)
                            .map(tuple -> {
                                Map<String, AuthUser> usersMap = tuple.getT1();
                                Map<Integer, LoanType> loanTypesMap = tuple.getT2();

                                List<LoanDetail> loanDetails = loans.stream()
                                        .map(loan -> {
                                            AuthUser user = usersMap.get(loan.getUserEmail());
                                            LoanType loanType = loanTypesMap.get(loan.getLoanTypeId());
                                            return new LoanDetail(loan, user, loanType);
                                        })
                                        .toList();

                                return new PaginatedResult<>(
                                        loanDetails,
                                        paginatedResult.totalElements(),
                                        paginatedResult.totalPages(),
                                        paginatedResult.currentPage(),
                                        paginatedResult.pageSize()
                                );
                            });
                });
    }
}