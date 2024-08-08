package com.example.mailbox.Service;


import com.example.mailbox.Entity.Mailbox;
import com.example.mailbox.Repository.ImailBoxRepo;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.transaction.Transactional;
import java.util.List;

@Service
@AllArgsConstructor
@RequiredArgsConstructor
public class MailBoxService {
    private static final Logger logger = LoggerFactory.getLogger(MailBoxService.class);

    @Autowired
    private ImailBoxRepo mailboxRepository;
    @Autowired
    private MailServiceClient mailServiceClient;
//    @Autowired
//    private RestTemplate restTemplate;
    private SseEmitter emitter;
    private final RestTemplate restTemplate = new RestTemplate();


//    @Autowired
//     private IMailRepository emailRepository;
//    @Autowired
//    private IEmailService emailService;

    public List<Mailbox> getMailboxesByUserId(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        List<Mailbox> mailboxes = mailboxRepository.findByUserId(userId);
        if (mailboxes == null) {
            throw new RuntimeException("Mailboxes not found");
        }
        return mailboxes;
    }

    @Transactional
    public Mailbox createMailboxAndFetchEmails(String userEmail, String userPassword, Integer userId) {
        // Create the mailbox
        Mailbox mailbox = new Mailbox();
        mailbox.setEmailAddress(userEmail);
        mailbox.setPassword(userPassword);
        mailbox.setUserId(userId);
        Mailbox savedMailbox = mailboxRepository.save(mailbox);

        // Start fetching emails
        fetchAndSaveEmails(savedMailbox.getId(), userEmail, userPassword);

        return savedMailbox;
    }

    private void fetchAndSaveEmails(Long mailboxId, String email, String password) {
        String mailServiceUrl = "http://localhost:8083/email/api/v1/emails/fetch";
        ResponseEntity<String> response = restTemplate.postForEntity(
                mailServiceUrl + "?mailboxId=" + mailboxId + "&email=" + email + "&password=" + password,
                null, String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            // Process the response or handle success case
        } else {
            throw new RuntimeException("Failed to fetch emails from mail service");
        }
    }



    public Boolean fetchEmailsForMailbox1(String userEmail, String userPassword) {
        String url = "http://localhost:8083/email/api/v1/emails/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("userEmail", userEmail);
        map.add("userPassword", userPassword);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            // Log the exception and return false
            System.err.println("Error verifying credentials: " + e.getMessage());
            return false;
        }
    }



    public void fetchEmailsForAllMailboxes(Integer userId) {
        List<Mailbox> mailboxes = mailboxRepository.findByUserId(userId);
        for (Mailbox mailbox : mailboxes) {
            fetchEmailsForMailbox1(mailbox.getEmailAddress(), mailbox.getPassword());
        }
    }
    public void triggerEmailFetch(Long mailboxId) {
        String url = "http://localhost:8083/email/api/v1/emails/fetch?mailboxId=" + mailboxId;
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                // Handle successful response
                System.out.println("Emails fetching process started successfully.");
            } else {
                // Handle error response
                System.err.println("Failed to start emails fetching process: " + response.getBody());
            }
        } catch (Exception e) {
            // Handle exception
            e.printStackTrace();
        }
    }
    @Transactional
    public void deleteMailbox(Long mailboxId) {
        if (mailboxRepository.existsById(mailboxId)) {
            mailboxRepository.deleteById(mailboxId);
        } else {
            throw new RuntimeException("Mailbox with ID " + mailboxId + " does not exist.");
        }
    }


}


