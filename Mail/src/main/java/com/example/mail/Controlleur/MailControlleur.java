package com.example.mail.Controlleur;

import com.example.mail.Entity.Mail;
import com.example.mail.Repository.IMailRepository;
import com.example.mail.Service.EmailService;
import com.example.mail.Service.IEmailService;
import com.example.mail.Service.SessionService;
import com.sun.mail.imap.IMAPFolder;
import jakarta.mail.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import jakarta.mail.search.HeaderTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;



@RestController
@RequestMapping("api/v1/emails")
//@CrossOrigin(origins = "*")
public class MailControlleur {
    @Autowired
    private SessionService sessionService;


    public static final Map<Long, Integer> progressMap = new ConcurrentHashMap<>();

    @Autowired
    private IEmailService emailService;
    @Autowired
    private IMailRepository mailRepository;
    private static final Logger logger = LoggerFactory.getLogger(MailControlleur.class);

    @PostMapping("/fetch")
    public ResponseEntity<String> fetchEmails(
            @RequestParam Long mailboxId,
            @RequestParam String email,
            @RequestParam String password) {

        progressMap.put(mailboxId, 0);

        // Start email fetching in a new thread
        new Thread(() -> {
            try {
                emailService.readAndSaveEmails33(mailboxId, email, password);
            } catch (Exception e) {
                // Log and handle the exception
                e.printStackTrace(); // Use a proper logging framework
            } finally {
                progressMap.remove(mailboxId); // Clean up progress tracking
            }
        }).start();

        return ResponseEntity.ok("Email fetching started.");
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/progress/{mailboxId}")
    public SseEmitter getProgressEmitter(@PathVariable Long mailboxId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Set a very long timeout

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        final long startTime = System.currentTimeMillis();
        final long EXPECTED_PROCESSING_TIME = 24 * 60 * 60 * 1000L;

        emitter.onCompletion(() -> System.out.println("Emitter completed for mailbox ID: " + mailboxId));
        emitter.onError(throwable -> System.err.println("Emitter error for mailbox ID: " + mailboxId + ", " + throwable.getMessage()));
        emitter.onTimeout(() -> {
            System.out.println("Emitter timeout for mailbox ID: " + mailboxId);
            emitter.complete(); // Complete the emitter on timeout
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                Integer progress = progressMap.get(mailboxId);
                if (progress != null) {
                    emitter.send(SseEmitter.event().name("progress").data(progress));
                    System.out.println("Progress sent: " + progress + " for mailbox ID: " + mailboxId);

                    // Extend timeout if processing is still ongoing
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    if (elapsedTime < EXPECTED_PROCESSING_TIME) {
                        emitter.send(SseEmitter.event().name("extendTimeout").data(elapsedTime));
                    } else {
                        emitter.complete();
                    }
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 0, 1, TimeUnit.SECONDS);

        return emitter;
    }

    @PostMapping("/check")
    public ResponseEntity<String> checkNewEmails(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Long mailboxId) {
        try {
            emailService.checkAndLogNewEmails(email, password, mailboxId);
            return ResponseEntity.ok("Email check completed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error checking emails: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Mail>> getAllEmails(
            @RequestParam Long mailboxId) {
        if (mailboxId == null) {
            return ResponseEntity.badRequest().body(null);
        }
        try {
            List<Mail> emails = emailService.findByMailboxId(mailboxId);
            return ResponseEntity.ok(emails);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @PostMapping("/archive")
    public ResponseEntity<String> archiveEmail(
            @RequestParam String mailUid,
            @RequestParam Long mailboxId) {
        logger.info("Archiving email with UID: {} and mailbox ID: {}", mailUid, mailboxId);

        try {
            String decodedUid = URLDecoder.decode(mailUid, StandardCharsets.UTF_8);
            Mail mail = mailRepository.findByUid(decodedUid);
            if (mail == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found with UID: " + decodedUid);
            }

            Store store = sessionService.getSession(mailboxId);
            if (store == null || !store.isConnected()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Session not found or disconnected for mailboxId: " + mailboxId);
            }

            // Reuse folders if possible
            Folder inbox = store.getFolder("INBOX");
            if (!inbox.isOpen()) {
                inbox.open(Folder.READ_WRITE);
            }

            Folder archiveFolder = store.getFolder("Archive");
            if (!archiveFolder.exists()) {
                archiveFolder.create(Folder.HOLDS_MESSAGES);
            }
            if (!archiveFolder.isOpen()) {
                archiveFolder.open(Folder.READ_WRITE);
            }

            // Optimize UID search
            Message message = findMessageByUid(inbox, decodedUid);
            if (message != null) {
                archiveFolder.appendMessages(new Message[]{message});
                message.setFlag(Flags.Flag.DELETED, true);
                mail.setArchived(true);
                mailRepository.save(mail);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found with UID: " + decodedUid);
            }

        } catch (MessagingException e) {
            logger.error("Error archiving email with UID: " + mailUid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error archiving email with UID: " + mailUid);
        }

        return ResponseEntity.ok("Email archived successfully");
    }

    public Message findMessageByUid(Folder folder, String uidString) throws MessagingException {
        logger.info("Searching for message with UID: {}", uidString);
        Message[] messages = folder.search(new HeaderTerm("Message-ID", uidString));
        if (messages != null && messages.length > 0) {
            logger.info("Message found with UID: {}", uidString);
            return messages[0];
        } else {
            logger.warn("No message found with UID: {}", uidString);
        }
        return null;
    }


    @GetMapping("/archived-emails")
    public ResponseEntity<List<Mail>> getArchivedEmails(@RequestParam Long mailboxId) {
        List<Mail> archivedEmails = mailRepository.findByMailboxIdAndAndArchived(mailboxId, true);
        return ResponseEntity.ok(archivedEmails);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteEmail(
            @RequestParam String mailUid,
            @RequestParam Long emailId,
            @RequestParam Long mailboxId) {
        logger.info("Deleting email with UID: {} and mailbox ID: {}", mailUid, mailboxId);

        try {
            String decodedUid = URLDecoder.decode(mailUid, StandardCharsets.UTF_8);
            logger.info("Decoded UID: {}", decodedUid);

            Mail mail = mailRepository.findByUid(decodedUid);
            if (mail == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found with UID: " + decodedUid);
            }

            Store store = sessionService.getSession(mailboxId);
            if (store == null || !store.isConnected()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Session not found or disconnected for mailboxId: " + mailboxId);
            }

            Folder inbox = store.getFolder("INBOX");
            if (!inbox.isOpen()) {
                inbox.open(Folder.READ_WRITE);
            }

            Message message = findMessageByUid(inbox, decodedUid);
            if (message != null) {
                message.setFlag(Flags.Flag.DELETED, true);
                inbox.close(true); // Expunge the deleted messages
                store.close();
                mailRepository.deleteById(emailId); // Delete email from database
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email not found with UID: " + decodedUid);
            }

        } catch (MessagingException e) {
            logger.error("Error deleting email with UID: " + mailUid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting email with UID: " + mailUid);
        }

        return ResponseEntity.ok("Email deleted successfully");
    }
    @GetMapping("/top-domains-attachments")
    public List<Map<String, Object>> getTopDomainsWithAttachments() {
        return emailService.getTopDomainsWithAttachments();
    }
}
