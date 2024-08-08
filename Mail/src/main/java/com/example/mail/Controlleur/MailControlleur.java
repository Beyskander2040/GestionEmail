package com.example.mail.Controlleur;

import com.example.mail.Entity.EmailDTO;
import com.example.mail.Entity.Mail;
import com.example.mail.Repository.IMailRepository;
import com.example.mail.Service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("api/v1/emails")
@CrossOrigin(origins = "*")
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


    @GetMapping("/emails")
    public ResponseEntity<List<Mail>> getEmails(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info("Fetching emails with pagination - page: {}, size: {}", page, size);

        List<Mail> emails = emailService.fetchEmails(email, password, page, size);

        return ResponseEntity.ok(emails);
    }


    @PostMapping("/login")
    public ResponseEntity<List<EmailDTO>> loginAndFetchEmails(@RequestParam String userEmail, @RequestParam String userPassword) {
        List<EmailDTO> emails = emailService.fetchAndReturnEmails(userEmail, userPassword);
        if (emails != null) {
            return ResponseEntity.ok(emails);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }


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




}


