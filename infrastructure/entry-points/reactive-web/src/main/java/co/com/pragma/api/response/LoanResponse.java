package co.com.pragma.api.response;

import co.com.pragma.model.state.State;
import lombok.*;

/**
* Datos del user creado, excluyendo información sensible.
*/

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanResponse {
    private String userEmail;
    private String userIdNumber;
    private State state;
}