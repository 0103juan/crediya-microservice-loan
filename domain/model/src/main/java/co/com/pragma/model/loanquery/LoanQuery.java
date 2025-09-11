package co.com.pragma.model.loanquery;

import co.com.pragma.model.state.State;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

@Getter
@Builder(toBuilder = true)
public class LoanQuery {
    private final int page;
    private final int size;

    private final Optional<String> userEmail;
    private final Optional<String> userIdNumber;
    private final List<State> states;
}