package Unit00.com.Exalyze.dto;


import lombok.Data;

@Data
public class SignInRequest {
    private String email;
    private String password;
}