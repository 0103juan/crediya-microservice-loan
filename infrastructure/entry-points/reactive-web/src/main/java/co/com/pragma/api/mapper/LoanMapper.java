package co.com.pragma.api.mapper;


import co.com.pragma.api.dto.LoanDTO;
import co.com.pragma.api.request.RegisterLoanRequest;
import co.com.pragma.api.response.LoanDetailResponse;
import co.com.pragma.api.response.LoanResponse;
import co.com.pragma.model.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.loandetail.LoanDetail;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.state.State;
import co.com.pragma.model.loan.Loan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel= "spring")
public interface LoanMapper {

    LoanResponse toResponse(Loan loan);

    LoanDTO toDTO(Loan loan);

    List<LoanDTO> toListDTO(List<Loan> loans);

    @Mapping(target = "userEmail", ignore = true)
    @Mapping(target = "userIdNumber", ignore = true)
    @Mapping(target = "loanType", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "loanTypeId", source = "loanType")
    Loan toModel(RegisterLoanRequest registerLoanRequest);

    default State toState(String stateName) {
        try {
            return State.valueOf(stateName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidLoanTypeException("El estado '" + stateName + "' no es válido.");
        }
    }

    @Mapping(source = "loan.amount", target = "amount")
    @Mapping(source = "loan.term", target = "term")
    @Mapping(source = "loan.userEmail", target = "userEmail")
    @Mapping(source = "loanDetail", target = "userName", qualifiedByName = "toUserName")
    @Mapping(source = "loanType.name", target = "loanTypeName")
    @Mapping(source = "loanType.interestRate", target = "interestRate")
    @Mapping(source = "loan.state", target = "state")
    @Mapping(source = "loanDetail", target = "monthlyPayment", qualifiedByName = "calculateMonthlyPayment")
    @Mapping(source = "user.baseSalary", target = "baseSalary")
    LoanDetailResponse toLoanDetailResponse(LoanDetail loanDetail);

    List<LoanDetailResponse> toLoanDetailResponseList(List<LoanDetail> loanDetails);


    @Named("toUserName")
    default String toUserName(LoanDetail loanDetail) {
        if (loanDetail.getUser() == null) {
            return null;
        }
        return loanDetail.getUser().getFirstName() + " " + loanDetail.getUser().getLastName();
    }

    @Named("calculateMonthlyPayment")
    default BigDecimal calculateMonthlyPayment(LoanDetail loanDetail) {
        BigDecimal principal = loanDetail.getLoan().getAmount();
        LoanType loanType = loanDetail.getLoanType();
        int termInMonths = loanDetail.getLoan().getTerm();

        if (loanType == null || loanType.getInterestRate() == null || termInMonths <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal annualInterestRate = loanType.getInterestRate();
        BigDecimal monthlyRate = annualInterestRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termInMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal ratePowered = (BigDecimal.ONE.add(monthlyRate)).pow(termInMonths);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(ratePowered);
        BigDecimal denominator = ratePowered.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }


}
