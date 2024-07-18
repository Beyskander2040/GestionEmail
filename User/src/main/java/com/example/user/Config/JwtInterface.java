package com.example.user.Config;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

public interface JwtInterface {
    String extractUserName(String token);
    String generateToken(UserDetails userDetails);
    String generateToken(Map<String, Object> extraClaims, UserDetails userDetails);
    boolean isTokenValid(String token, UserDetails userDetails);
    void validateToken(String token);
}
