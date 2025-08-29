package co.com.pragma.model.authuser;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    private String firstName;
    private String lastName;
    private String email;
    private Long idNumber;
    private String description;
}