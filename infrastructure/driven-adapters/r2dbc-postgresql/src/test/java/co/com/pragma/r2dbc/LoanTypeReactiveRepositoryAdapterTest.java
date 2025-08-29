package co.com.pragma.r2dbc;

import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.r2dbc.entity.LoanTypeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanTypeReactiveRepositoryAdapterTest {

    @Mock
    private LoanTypeReactiveRepository repository;
    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private LoanTypeReactiveRepositoryAdapter repositoryAdapter;

    private LoanType loanType;
    private LoanTypeEntity loanTypeEntity;

    @BeforeEach
    void setUp() {
        // Objeto de dominio que usamos en los casos de uso
        loanType = LoanType.builder()
                .name("PERSONAL")
                .minimumAmount(BigDecimal.valueOf(1000))
                .maximumAmount(BigDecimal.valueOf(50000))
                .interestRate(BigDecimal.TEN)
                .automaticValidation(true)
                .build();

        // Entidad que representa la tabla en la base de datos
        loanTypeEntity = new LoanTypeEntity();
        loanTypeEntity.setId(1);
        loanTypeEntity.setName("PERSONAL");
        loanTypeEntity.setMinAmount(BigDecimal.valueOf(1000));
        loanTypeEntity.setMaxAmount(BigDecimal.valueOf(50000));
        loanTypeEntity.setInterestRate(BigDecimal.TEN);
        loanTypeEntity.setAutomaticValidation(true);
    }

    @Test
    @DisplayName("Debería guardar un tipo de préstamo exitosamente")
    void saveLoanType_Success() {
        // Simular el mapeo de Dominio -> Entidad
        when(mapper.map(any(LoanType.class), any(Class.class))).thenReturn(loanTypeEntity);
        // Simular la respuesta del repositorio al guardar
        when(repository.save(any(LoanTypeEntity.class))).thenReturn(Mono.just(loanTypeEntity));
        // Simular el mapeo de Entidad -> Dominio
        when(mapper.map(any(LoanTypeEntity.class), any(Class.class))).thenReturn(loanType);

        // Probar el método
        StepVerifier.create(repositoryAdapter.saveLoanType(loanType))
                .expectNextMatches(saved -> saved.getName().equals("PERSONAL"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Debería obtener un tipo de préstamo por su ID")
    void getLoanTypeById_Success() {
        when(repository.findById(any(Integer.class))).thenReturn(Mono.just(loanTypeEntity));
        when(mapper.map(any(LoanTypeEntity.class), any(Class.class))).thenReturn(loanType);

        StepVerifier.create(repositoryAdapter.getLoanTypeById(1))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("Debería devolver un Mono vacío si el ID no existe")
    void getLoanTypeById_NotFound() {
        when(repository.findById(any(Integer.class))).thenReturn(Mono.empty());

        StepVerifier.create(repositoryAdapter.getLoanTypeById(99))
                // Verifica que el Mono complete sin emitir ningún elemento
                .verifyComplete();
    }

    @Test
    @DisplayName("Debería propagar errores del repositorio al buscar por ID")
    void getLoanTypeById_RepositoryError() {
        when(repository.findById(any(Integer.class))).thenReturn(Mono.error(new RuntimeException("Error de base de datos")));

        StepVerifier.create(repositoryAdapter.getLoanTypeById(1))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("Debería obtener todos los tipos de préstamo")
    void getAllLoansType_Success() {
        when(repository.findAll()).thenReturn(Flux.just(loanTypeEntity, loanTypeEntity));
        when(mapper.map(any(LoanTypeEntity.class), any(Class.class))).thenReturn(loanType);

        StepVerifier.create(repositoryAdapter.getAllLoansType())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Debería devolver un Flux vacío si no hay tipos de préstamo")
    void getAllLoansType_Empty() {
        when(repository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(repositoryAdapter.getAllLoansType())
                // Verifica que el Flux complete sin emitir ningún elemento
                .verifyComplete();
    }
}