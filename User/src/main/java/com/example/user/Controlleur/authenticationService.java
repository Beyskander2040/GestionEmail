package com.example.user.Controlleur;

import com.example.user.Config.jwtService;
import com.example.user.Controlleur.AuthenticationRequest;
import com.example.user.Controlleur.AuthenticationResponse;
import com.example.user.Controlleur.RegisterRequest;
import com.example.user.Entity.User;
import com.example.user.Repository.IuserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class authenticationService {
    private final IuserRepository iuserRepository ;
    private final PasswordEncoder passwordEncoder ;
    private final jwtService jwtService;
    private final AuthenticationManager  authenticationManager ;

    public AuthenticationResponse register(RegisterRequest request) {
        User user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        var savedUser = iuserRepository.save(user);
var jwToken = jwtService.generateToken(savedUser);
        return AuthenticationResponse.builder()
                .token(jwToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
var user = iuserRepository.findByEmail(request.getEmail())
        .orElseThrow();

        var jwToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwToken)
                .build();

    }

}
