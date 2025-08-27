package co.com.pragma.usecase.registerloan;

import co.com.pragma.model.authuser.gateways.AuthUserRepository;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.exceptions.LoanValidationException;
import co.com.pragma.model.exceptions.UserNotFoundException;
import co.com.pragma.model.loan.Loan;
import co.com.pragma.model.loan.gateways.LoanRepository;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.model.state.State;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Map;

@RequiredArgsConstructor
public class RegisterLoanUseCase {

    private final LoanRepository loanRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final AuthUserRepository authRepository;

    public Mono<Loan> saveLoan(Loan loan, Integer loanTypeId){

        // 1. Validar que el usuario exista en el microservicio de autenticación
        return authRepository.findByIdNumber(loan.getUserIdNumber())
                .switchIfEmpty(Mono.error(new UserNotFoundException("El usuario con documento " + loan.getUserIdNumber() + " no está registrado.")))
                .flatMap(authUser -> {
                    // 2. Validar que el email coincida
                    if (!authUser.getEmail().equalsIgnoreCase(loan.getUserEmail())) {
                        return Mono.error(new LoanValidationException("El correo electrónico no coincide con el del usuario registrado.", Map.of("userEmail", "El correo no es válido para el documento proporcionado.")));
                    }

                    // 3. Continuar con la lógica existente: validar tipo de préstamo
                    return loanTypeRepository.getLoanTypeById(loanTypeId);
                })
                .switchIfEmpty(Mono.error(new InvalidLoanTypeException("El tipo de préstamo con ID " + loanTypeId + " no existe.")))
                .flatMap(loanType -> {
                    loan.setLoanType(loanType);
                    loan.setState(State.REVIEW_PENDING);

                    // 4. Guardar el préstamo
                    return loanRepository.saveLoan(loan);
                });
    }
}
