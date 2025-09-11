package co.com.pragma.api;

import co.com.pragma.api.mapper.LoanMapper;
import co.com.pragma.api.mapper.LoanQueryMapper;
import co.com.pragma.api.request.RegisterLoanRequest;
import co.com.pragma.api.response.ApiResponse;
import co.com.pragma.api.response.CustomStatus;
import co.com.pragma.api.response.LoanDetailResponse;
import co.com.pragma.api.response.LoanResponse;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loanquery.LoanQuery;
import co.com.pragma.model.paginatedresult.PaginatedResult;
import co.com.pragma.requestvalidator.RequestValidator;
import co.com.pragma.usecase.findloans.FindLoansUseCase;
import co.com.pragma.usecase.registerloan.RegisterLoanUseCase;
import lombok.extern.log4j.Log4j2;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.net.URI;
import java.util.List;

@Log4j2
@AllArgsConstructor
@Component
public class LoanApiHandler {
    private final RegisterLoanUseCase registerLoanUseCase;
    private final FindLoansUseCase findLoansUseCase;
    private final LoanMapper loanMapper;
    private final RequestValidator validator;

    public Mono<ServerResponse> listenRegister(ServerRequest serverRequest) {
        log.info("Recibida petición para registrar nueva solicitud en la ruta: {}", serverRequest.path());

        Mono<String> userEmailMono = ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName());

        return userEmailMono.flatMap(userEmail ->
                serverRequest.bodyToMono(RegisterLoanRequest.class)
                        .flatMap(validator::validate)
                        .map(request -> {
                            log.info("Petición de registro válida. Mapeando a modelo de dominio.");
                            Loan loanModel = loanMapper.toModel(request);
                            loanModel.setUserEmail(userEmail);
                            return Tuples.of(loanModel, request.getLoanType());
                        })
                        .flatMap(tuple -> {
                            Loan loanToSave = tuple.getT1();
                            Integer loanTypeId = tuple.getT2();
                            log.info("Invocando caso de uso 'save' para el usuario: {}", loanToSave.getUserEmail());
                            return registerLoanUseCase.save(loanToSave, loanTypeId);
                        })
                .flatMap(savedLoan -> {
                    LoanResponse loanResponse = loanMapper.toResponse(savedLoan);
                    URI location = URI.create(serverRequest.uri() + "/" + savedLoan.getUserIdNumber());
                    CustomStatus status = CustomStatus.LOAN_REQUEST_SUCCESSFULLY;

                    ApiResponse<LoanResponse> apiResponse = ApiResponse.<LoanResponse>builder()
                            .code(status.getCode())
                            .message(status.getMessage())
                            .path(location.toString())
                            .data(loanResponse)
                            .build();

                    return ServerResponse.created(location)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(apiResponse);
                })
        );
    }


    public Mono<ServerResponse> listenFindAllByStatus(ServerRequest serverRequest) {
        log.info("Recibida petición de Asesor para obtener listado de solicitudes para revisión.");
        // Usamos el nuevo mapper y el nuevo objeto
        LoanQuery query = LoanQueryMapper.from(serverRequest);

        return findLoansUseCase.findByStatus(query) // Pasamos el objeto query
                .flatMap(paginatedResult -> {
                    CustomStatus status = CustomStatus.LOANS_FOUND_SUCCESSFULLY;

                    PaginatedResult<LoanDetailResponse> responsePaginatedResult = new PaginatedResult<>(
                            loanMapper.toLoanDetailResponseList(paginatedResult.content()),
                            paginatedResult.totalElements(),
                            paginatedResult.totalPages(),
                            paginatedResult.currentPage(),
                            paginatedResult.pageSize()
                    );

                    ApiResponse<LoanDetailResponse> apiResponse = ApiResponse.<LoanDetailResponse>builder()
                            .code(status.getCode())
                            .message(status.getMessage())
                            .pages(responsePaginatedResult)
                            .path(serverRequest.path())
                            .build();

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(apiResponse);
                });
    }
}