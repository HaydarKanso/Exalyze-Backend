package unit00.com.exalyze.dto;


import lombok.Data;

@Data
public class SignInRequest {
    private String email;
    private String password;
}