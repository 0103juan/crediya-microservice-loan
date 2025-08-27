package co.com.pragma.config;

import co.com.pragma.model.loan.gateways.LoanRepository;
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
    }

    @Test
    void useCaseBeansShouldBeCreated() {
        // Se ejecuta el contexto con la configuración de UseCases y la de prueba.
        contextRunner.withUserConfiguration(UseCasesConfig.class, TestConfig.class)
                .run(context -> {
                    // Se verifica que los beans de los casos de uso reales existan en el contexto.
                    assertThat(context).hasBean("registerLoanUseCase");

                    // Se obtienen los beans para asegurar que no son nulos.
                    RegisterLoanUseCase registerLoanUseCase = context.getBean(RegisterLoanUseCase.class);

                    assertThat(registerLoanUseCase).isNotNull();

                    // Opcional: Verificar que no exista un bean que no debería estar.
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