package co.com.pragma.api;

import co.com.pragma.api.config.LoanPath;
import co.com.pragma.api.mapper.LoanMapper;
import co.com.pragma.api.request.RegisterLoanRequest;
import co.com.pragma.api.response.ApiResponse;
import co.com.pragma.api.response.CustomStatus;
import co.com.pragma.api.response.LoanDetailResponse;
import co.com.pragma.api.response.LoanResponse;
import co.com.pragma.model.authuser.AuthUser;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loandetail.LoanDetail;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.paginatedresult.PaginatedResult;
import co.com.pragma.model.state.State;
import co.com.pragma.requestvalidator.RequestValidator;
import co.com.pragma.usecase.findloans.FindLoansUseCase;
import co.com.pragma.usecase.registerloan.RegisterLoanUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(excludeAutoConfiguration = {ReactiveSecurityAutoConfiguration.class})
@ContextConfiguration(classes = {LoanApiRouter.class, LoanApiHandler.class, LoanApiHandlerTest.TestConfig.class})
class LoanApiHandlerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RegisterLoanUseCase registerLoanUseCase() {
            return Mockito.mock(RegisterLoanUseCase.class);
        }

        // <-- Cambio aquí: Añadir el mock para FindLoansUseCase
        @Bean
        public FindLoansUseCase findLoansUseCase() {
            return Mockito.mock(FindLoansUseCase.class);
        }

        @Bean
        public RequestValidator requestValidator() {
            return Mockito.mock(RequestValidator.class);
        }
        @Bean
        public LoanMapper loanMapper() {
            return Mappers.getMapper(LoanMapper.class);
        }
        @Bean
        public LoanPath loanPath() {
            LoanPath loanPath = new LoanPath();
            loanPath.setLoans("/api/v1/loans");
            return loanPath;
        }
    }

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private RegisterLoanUseCase registerLoanUseCase;
    @Autowired
    private FindLoansUseCase findLoansUseCase; // <-- Cambio aquí: Inyectar el nuevo use case
    @Autowired
    private RequestValidator requestValidator;

    private RegisterLoanRequest validRequest;
    private Loan loanDomain;
    private final String mockUserEmail = "cliente@pragma.com";

    @BeforeEach
    void setUp() {
        validRequest = new RegisterLoanRequest();
        validRequest.setAmount(BigDecimal.valueOf(20000));
        validRequest.setTerm(24);
        validRequest.setLoanType(2);

        loanDomain = Mappers.getMapper(LoanMapper.class).toModel(validRequest);
        loanDomain.setUserEmail(mockUserEmail);
        loanDomain.setState(State.REVIEW_PENDING);
    }

    @Test
    void registerLoan_whenRequestIsValid_shouldReturnCreated() {
        when(requestValidator.validate(any(RegisterLoanRequest.class))).thenReturn(Mono.just(validRequest));
        when(registerLoanUseCase.save(any(Loan.class), any(Integer.class))).thenReturn(Mono.just(loanDomain));
        CustomStatus expectedStatus = CustomStatus.LOAN_REQUEST_SUCCESSFULLY;

        webTestClient
                .mutateWith(mockUser(mockUserEmail).roles("CLIENTE"))
                .post()
                .uri("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(new ParameterizedTypeReference<ApiResponse<LoanResponse>>() {})
                .value(apiResponse -> {
                    assertThat(apiResponse.getCode()).isEqualTo(expectedStatus.getCode());
                    assertThat(apiResponse.getData()).isNotNull();
                    assertThat(apiResponse.getData().getUserEmail()).isEqualTo(mockUserEmail);
                    assertThat(apiResponse.getData().getState()).isEqualTo(State.REVIEW_PENDING);
                });
    }

    // <-- Cambio aquí: Nuevo test para el endpoint GET
    @Test
    void findLoansForReview_Success() {
        LoanDetail loanDetail = LoanDetail.builder()
                .loan(Loan.builder().userEmail("test@test.com").amount(BigDecimal.TEN).term(12).state(State.MANUAL_REVIEW).build())
                .user(AuthUser.builder().firstName("Test").lastName("User").build())
                .loanType(LoanType.builder().name("PERSONAL").build())
                .build();

        PaginatedResult<LoanDetail> paginatedResult = new PaginatedResult<>(List.of(loanDetail), 1, 1, 0, 10);

        when(findLoansUseCase.findByStatus(any())).thenReturn(Mono.just(paginatedResult));

        CustomStatus expectedStatus = CustomStatus.LOANS_FOUND_SUCCESSFULLY;

        webTestClient
                .mutateWith(mockUser("asesor@pragma.com").roles("ASESOR"))
                .get()
                .uri("/api/v1/loans?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<LoanDetailResponse>>() {})
                .value(apiResponse -> {
                    assertThat(apiResponse.getCode()).isEqualTo(expectedStatus.getCode());
                    assertThat(apiResponse.getPages()).isNotNull();
                    assertThat(apiResponse.getPages().content()).hasSize(1);
                    assertThat(apiResponse.getPages().content().getFirst().getUserEmail()).isEqualTo("test@test.com");
                });
    }
}