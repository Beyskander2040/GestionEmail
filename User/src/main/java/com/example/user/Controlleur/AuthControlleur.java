package com.example.user.Controlleur;

import com.example.user.Config.JwtInterface;
import com.example.user.Entity.EmailCredential;
import com.example.user.Entity.User;
import com.example.user.Repository.IuserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthControlleur {
    private final authenticationService authenticationService;
    private final JwtInterface jwtInterface;
    private final IuserRepository iuserRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));

    }

    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        jwtInterface.validateToken(token);
        return "token is valid";

    }

//    @GetMapping("/email-credentials")
//    public ResponseEntity<List<EmailCredential>> fetchEmailCredentials(@RequestParam("email") String email) {
//        // Retrieve user from user repository
//        Optional<User> optionalUser = iuserRepository.findByEmail(email);
//        if (optionalUser.isEmpty()) {
//            // Handle case where user with given username/email does not exist
//            return ResponseEntity.notFound().build();
//        }
//
//        User user = optionalUser.get();
//
//        // Retrieve email credentials from authentication service
//        List<EmailCredential> credentials = authenticationService.getEmailCredentials(email);
//        if (credentials.isEmpty()) {
//            // Handle case where no email credentials are found
//            return ResponseEntity.noContent().build();
//        }
//
//        // Return successful response with email credentials
//        return ResponseEntity.ok(credentials);
//    }


    @PostMapping("/{userId}/additional-emails")
    public ResponseEntity<String> addAdditionalEmail(
            @PathVariable Integer userId,
            @RequestParam String additionalEmail) {

        Optional<User> optionalUser = iuserRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            List<String> mailAddresses = user.getAdditionalEmails();

            if (!mailAddresses.contains(additionalEmail)) {
                mailAddresses.add(additionalEmail);
                user.setAdditionalEmails(mailAddresses);
                iuserRepository.save(user);
                return ResponseEntity.ok("Additional email added successfully.");
            } else {
                return ResponseEntity.badRequest().body("Email already exists.");
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{userId}/email-addresses")
    public ResponseEntity<List<String>> getEmailAddresses(@PathVariable Integer userId) {
        Optional<User> optionalUser = iuserRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            List<String> mailAddresses = user.getAdditionalEmails();
            return ResponseEntity.ok(mailAddresses);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Integer id) {
        return iuserRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }
    @GetMapping("/by-username")
    public User getUserByUsername(@RequestParam String username) {
        return iuserRepository.findByEmail(username).orElseThrow(() -> new RuntimeException("User not found"));
    }
}