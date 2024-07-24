package com.example.mailbox.Service;

import com.example.mailbox.Entity.Mailbox;
import com.example.mailbox.Repository.ImailBoxRepo;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@AllArgsConstructor
@RequiredArgsConstructor
public class MailBoxService {
    @Autowired
    private ImailBoxRepo mailboxRepository;
    @Autowired
    private MailServiceClient mailServiceClient;
    @Autowired
    private RestTemplate restTemplate;
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
    public Mailbox createMailbox(String userEmail, String userPassword, Integer userId) {
        // Create a new Mailbox entity
        Mailbox mailbox = new Mailbox();
        mailbox.setEmailAddress(userEmail);
        mailbox.setPassword(userPassword);
        mailbox.setUserId(userId);

        // Save the mailbox to the database
        mailbox = mailboxRepository.save(mailbox);

        // Fetch emails for the newly created mailbox
        fetchEmailsForMailbox(mailbox);

        return mailbox;
    }



    private void fetchEmailsForMailbox(Mailbox mailbox) {
        // Logic to fetch emails based on mailbox credentials
        String url = "http://localhost:8083/email/api/v1/emails/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("userEmail", mailbox.getEmailAddress());
        map.add("userPassword", mailbox.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            // Handle failure
            throw new RuntimeException("Failed to fetch emails for mailbox: " + response.getBody());
        }
    }

    public void fetchEmailsForMailbox1(String userEmail, String userPassword) {
        String url = "http://localhost:8083/email/api/v1/emails/fetch";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("userEmail", userEmail);
        map.add("userPassword", userPassword);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to fetch emails for mailbox: " + userEmail);
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


}


