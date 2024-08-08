package com.example.mailbox.Controlleur;

import com.example.mailbox.Entity.Mailbox;
import com.example.mailbox.Repository.ImailBoxRepo;
import com.example.mailbox.Service.MailBoxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/mailboxes")
public class MialBoxControlleur {
    @Autowired
   private  ImailBoxRepo imailBoxRepo ;

    @Autowired
    private MailBoxService  mailBoxService ;

    @Autowired
    private RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(MialBoxControlleur.class);

    @GetMapping("/getMailBoxByuser")
    public List<Mailbox> getMailboxes(@RequestHeader("userId") Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        System.out.println("User ID: " + userId);
        List<Mailbox> mailboxes = mailBoxService.getMailboxesByUserId(userId);
        if (mailboxes == null) {
            throw new RuntimeException("Mailboxes not found");
        }
        System.out.println("Mailboxes: " + mailboxes);
        return mailboxes;
    }

    @PostMapping("/createWithProgress")
    public ResponseEntity<Mailbox> createMailboxWithProgress(
            @RequestParam String userEmail,
            @RequestParam String userPassword,
            @RequestHeader Integer  userId) {

        logger.info("createMailboxWithProgress method called with userEmail: {}, userId: {}", userEmail, userId);

        try {
            Mailbox mailbox = mailBoxService.createMailboxAndFetchEmails(userEmail, userPassword, userId);
            logger.info("Mailbox created successfully with ID: {}", mailbox.getId());
            return ResponseEntity.ok(mailbox);
        } catch (Exception e) {
            logger.error("Error creating mailbox or fetching emails", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("/progress/{mailboxId}")
    public SseEmitter getProgressEmitter(@PathVariable Long mailboxId) {
        String mailServiceUrl = "http://localhost:8083/email/api/v1/emails/progress/" + mailboxId;
        return restTemplate.getForObject(mailServiceUrl, SseEmitter.class);
    }

    @GetMapping("/emails")
    public ResponseEntity<String> getEmailsForUser(@RequestHeader("userId") Integer userId) {
        try {
            String url = "http://localhost:8083/email/api/v1/emails/user/" + userId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch emails for user.");
            }

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
    @GetMapping("/fetchEmailsForUser/{userId}")
    public ResponseEntity<String> fetchEmailsForUser(@PathVariable Integer userId) {
        try {
            mailBoxService.fetchEmailsForAllMailboxes(userId);
            return ResponseEntity.ok("Emails fetched successfully for all mailboxes of user with ID: " + userId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteMailbox(@PathVariable("id") Long mailboxId) {
        try {
            mailBoxService.deleteMailbox(mailboxId);
            return ResponseEntity.ok("Mailbox deleted successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}

