package com.example.mail.Controlleur;

import com.example.mail.Entity.Mail;
import com.example.mail.Repository.IMailRepository;
import com.example.mail.Service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
@RestController
@RequestMapping("api/v1/emails")
//@CrossOrigin(origins = "*")
public class MailControlleur {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Logger logger = LogManager.getLogger(MailControlleur.class);
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    public  static  final Map<Long, Integer> progressMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isEmitterClosed = new AtomicBoolean(false);
    private final Object lock = new Object();

    private SseEmitter emitter;
    private final Object emitterLock = new Object();

    @Autowired
    private IMailRepository emailRepository;

    @Autowired
    private IEmailService emailService;
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
            logger.error("Error checking emails", e);  // Enhanced logging
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
}


