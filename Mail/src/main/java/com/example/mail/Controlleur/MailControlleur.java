package com.example.mail.Controlleur;

import com.example.mail.Entity.Mail;
import com.example.mail.Repository.IMailRepository;
import com.example.mail.Service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.logging.log4j.Logger;
import java.util.List;
import org.apache.logging.log4j.LogManager;


@RestController
@RequestMapping("api/v1/emails")
//@CrossOrigin(origins = "http://localhost:4200")
public class MailControlleur {
    private static final Logger logger = LogManager.getLogger(MailControlleur.class);
    @Autowired
    private IMailRepository emailRepository;

    @Autowired
    private IEmailService  emailService;


    @GetMapping("/emails")
    public ResponseEntity<List<Mail>> getEmails(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info("Fetching emails with pagination - page: {}, size: {}", page, size);

        List<Mail> emails = emailService.fetchEmails( email, password, page, size);

        return ResponseEntity.ok(emails);
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginAndValidate(
            @RequestParam String userEmail,
            @RequestParam String userPassword) {
        try {
            boolean isValid = emailService.readAndSaveEmails1(userEmail, userPassword);
            if (isValid) {
                return ResponseEntity.ok("Login successful.");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Mail>> getEmailsForUser(@PathVariable Integer userId) {
        try {
            List<Mail> emails = emailService.getEmailsByUserId(userId);
            return ResponseEntity.ok(emails);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

@PostMapping("/fetch")
public ResponseEntity<String> fetchEmails(
        @RequestParam Long mailboxId,
        @RequestParam String email,
        @RequestParam String password) {
    try {
        emailService.readAndSaveEmails33(mailboxId, email, password);
        return ResponseEntity.ok("Emails fetching process started successfully.");
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
    }
}

}

