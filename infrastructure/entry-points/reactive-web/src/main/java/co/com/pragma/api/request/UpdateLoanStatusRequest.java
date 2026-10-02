package co.com.pragma.api.request;

import co.com.pragma.model.state.State;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLoanStatusRequest {

    @NotNull(message = "El nuevo estado no puede ser nulo.")
    private State state;
}
