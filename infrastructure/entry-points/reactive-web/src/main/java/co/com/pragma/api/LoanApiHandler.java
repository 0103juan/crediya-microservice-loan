package co.com.pragma.api;

import co.com.pragma.api.mapper.LoanMapper;
import co.com.pragma.api.request.RegisterLoanRequest;
import co.com.pragma.api.response.ApiResponse;
import co.com.pragma.api.response.CustomStatus;
import co.com.pragma.api.response.LoanResponse;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.requestvalidator.RequestValidator;
import co.com.pragma.usecase.registerloan.RegisterLoanUseCase;
import lombok.extern.log4j.Log4j2;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

@Log4j2
@AllArgsConstructor
@Component
public class LoanApiHandler {
    private final RegisterLoanUseCase registerLoanUseCase;
    private final LoanMapper loanMapper;
    private final RequestValidator validator;

    public Mono<ServerResponse> listenRegister(ServerRequest serverRequest) {
        log.info("Recibida petición para registrar nueva solicitud en la ruta: {}", serverRequest.path());

        Mono<String> userEmailMono = ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName());

        return userEmailMono.flatMap(userEmail ->
                serverRequest.bodyToMono(RegisterLoanRequest.class)
                        .flatMap(validator::validate)
                        .flatMap(request -> {
                            log.info("Petición de registro válida para el usuario: {}, invocando caso de uso.", userEmail);
                            Loan loanModel = loanMapper.toModel(request);
                            loanModel.setUserEmail(userEmail);

                            return registerLoanUseCase.save(loanModel, request.getLoanType());
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

}