package co.com.pragma.webclient.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String idNumber;
}