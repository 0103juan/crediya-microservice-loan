package co.com.pragma.webclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthApiResponse<T> {
    private OffsetDateTime timestamp;
    private String code;
    private String message;
    private String path;
    private T data;
    private Map<String, List<String>> errors;
}