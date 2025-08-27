package co.com.pragma.api;

import co.com.pragma.api.mapper.LoanMapper;
import co.com.pragma.api.request.RegisterLoanRequest;
import co.com.pragma.api.response.LoanResponse;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.requestvalidator.RequestValidator;
import co.com.pragma.usecase.registerloan.RegisterLoanUseCase;
import lombok.extern.log4j.Log4j2;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
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
    private final LoanMapper userMapper;
    private final RequestValidator validator;

    public Mono<ServerResponse> listenRegister(ServerRequest serverRequest) {
        log.info("Recibida petición para registrar nueva solicitud en la ruta: {}", serverRequest.path());
        return serverRequest.bodyToMono(RegisterLoanRequest.class)
                .flatMap(validator::validate)
                .flatMap(request -> {
                    log.info("Petición de registro válida, invocando caso de uso RegisterLoanUseCase.");
                    Loan loanModel = userMapper.toModel(request);
                    return registerLoanUseCase.saveLoan(loanModel, request.getLoanType());
                })
                .flatMap(savedLoan -> {
                    LoanResponse userResponse = userMapper.toResponse(savedLoan);
                    URI location = URI.create(serverRequest.uri().toString() + "/" + savedLoan.getUserIdNumber());
                    log.info("Solicitud de préstamo registrada exitosamente. Location: {}", location);
                    return ServerResponse.created(location)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(userResponse);
                });
    }

}