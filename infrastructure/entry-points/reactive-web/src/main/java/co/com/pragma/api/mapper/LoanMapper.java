package co.com.pragma.api.mapper;


import co.com.pragma.api.dto.LoanDTO;
import co.com.pragma.api.request.RegisterLoanRequest;
import co.com.pragma.api.response.LoanResponse;
import co.com.pragma.model.exceptions.InvalidStateException;
import co.com.pragma.model.state.State;
import co.com.pragma.model.loan.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel= "spring")
public interface LoanMapper {

    @Mapping(target = "description", expression = "java(\"Usuario registrado exitosamente.\")")
    LoanResponse toResponse(Loan loan);

    LoanDTO toDTO(Loan loan);

    List<LoanDTO> toListDTO(List<Loan> loans);

    @Mapping(target = "loanType", ignore = true)
    @Mapping(target = "state", ignore = true)
    Loan toModel(RegisterLoanRequest registerLoanRequest);

    default State toState(String stateName) {
        try {
            return State.valueOf(stateName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidStateException("El estado '" + stateName + "' no es válido.");
        }
    }
}
