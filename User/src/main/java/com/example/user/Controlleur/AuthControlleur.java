package com.example.user.Controlleur;

import com.example.user.Config.JwtInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthControlleur {
    private final  authenticationService authenticationService;
    private final JwtInterface jwtInterface ;
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request){
        return ResponseEntity.ok(authenticationService.register(request));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody AuthenticationRequest request){
        return ResponseEntity.ok(authenticationService.authenticate(request));

}
    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        jwtInterface.validateToken(token);
        return "token is valid";

    }


}
