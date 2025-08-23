package Unit00.com.Exalyze.controller;

import  Unit00.com.Exalyze.dto.SignInRequest;
import  Unit00.com.Exalyze.dto.SignUpRequest;
 import Unit00.com.Exalyze.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody SignUpRequest signUpRequest) {
        try {
            userService.registerNewUser(signUpRequest.getEmail(), signUpRequest.getPassword());
            return new ResponseEntity<>("User registered successfully!", HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<String> signIn(@RequestBody SignInRequest signInRequest) {

        boolean isAuthenticated = userService.authenticateUser(signInRequest.getEmail(), signInRequest.getPassword());
        if (isAuthenticated) {
            return new ResponseEntity<>("Sign-in successful!", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invalid credentials.", HttpStatus.UNAUTHORIZED);
        }
    }
}