package co.com.pragma.config;

import co.com.pragma.model.authuser.gateways.AuthUserRepository;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.usecase.registerloan.RegisterLoanUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UseCasesConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Configuration
    static class TestConfig {
        @Bean
        public LoanRepository loanRepository() {
            return mock(LoanRepository.class);
        }

        @Bean
        public LoanTypeRepository loanTypeRepository() {
            return mock(LoanTypeRepository.class);
        }

        @Bean
        public AuthUserRepository authUserRepository() {
            return mock(AuthUserRepository.class);
        }
    }

    @Test
    void useCaseBeansShouldBeCreated() {
        contextRunner.withUserConfiguration(UseCasesConfig.class, TestConfig.class)
                .run(context -> {
                    assertThat(context).hasBean("registerLoanUseCase");

                    RegisterLoanUseCase registerLoanUseCase = context.getBean(RegisterLoanUseCase.class);

                    assertThat(registerLoanUseCase).isNotNull();

                    assertThat(context).doesNotHaveBean("myUseCase");
                });
    }

    @Test
    void useCaseBeansShouldNotBeCreatedWithoutDependencies() {
        contextRunner.withUserConfiguration(UseCasesConfig.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context).getFailure().hasCauseInstanceOf(NoSuchBeanDefinitionException.class);
                });
    }
}