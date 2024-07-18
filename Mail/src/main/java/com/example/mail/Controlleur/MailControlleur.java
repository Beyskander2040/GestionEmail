package com.example.mail.Controlleur;

import com.example.mail.Entity.Mail;
import com.example.mail.Repository.IMailRepository;
import com.example.mail.Service.EmailService;
import com.example.mail.Service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.logging.log4j.Logger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
@RestController
@RequestMapping("api/v1/emails")
@CrossOrigin(origins = "http://localhost:4200")
public class MailControlleur {
    private static final Logger logger = LogManager.getLogger(MailControlleur.class);
    @Autowired
    private IMailRepository emailRepository;

    @Autowired
    private IEmailService emailService;
//    @GetMapping
//  public List<Mail> getEmails() {
//        return emailService.getAllEmails();
//   }


    @GetMapping("/emails")
    public ResponseEntity<List<Mail>> getEmails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info("Fetching emails with pagination - page: {}, size: {}", page, size);

        // Start reading and saving emails asynchronously
        CompletableFuture<Void> asyncTask = CompletableFuture.runAsync(() -> {
            try {
                emailService.readAndSaveEmails();
            } catch (Exception e) {
                logger.error("Error while reading and saving emails asynchronously", e);
            }
        });

        // Fetch emails from the database with pagination
        Pageable pageable = PageRequest.of(page, size);
        Page<Mail> emailPage = emailService.getEmails(page, size); // Adjust this call as per your service method

        logger.info("Fetched {} emails from database", emailPage.getContent().size());

        return new ResponseEntity<>(emailPage.getContent(), HttpStatus.OK);
    }

//    @GetMapping("/read")
//    public ResponseEntity<String> readEmails() {
//        try {
//            emailService.readEmails();
//            return ResponseEntity.ok("Emails read successfully");
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Error reading emails: " + e.getMessage());
//        }
//    }
    @GetMapping("/test-log")
    public ResponseEntity<String> testLog() {
        logger.info("Test log endpoint hit");
        return ResponseEntity.ok("Log test successful");
    }
}
