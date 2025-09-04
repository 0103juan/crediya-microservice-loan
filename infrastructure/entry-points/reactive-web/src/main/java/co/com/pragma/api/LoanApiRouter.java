package co.com.pragma.api;

import co.com.pragma.api.config.LoanPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Log4j2
@RequiredArgsConstructor
@Configuration
public class LoanApiRouter {

    private final LoanPath loanPath;
    private final LoanApiHandler loanApiHandler;

    @Bean
    public RouterFunction<ServerResponse> routerFunction() {
        return route(POST(loanPath.getLoans()), loanApiHandler::listenRegister)
                .andRoute(GET(loanPath.getLoans()), loanApiHandler::listenFindAll);
    }
}
