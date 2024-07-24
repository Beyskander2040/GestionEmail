package com.example.mailbox.Service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mail-service", url = "http://localhost:8083")  // Adjust URL if necessary
public interface MailServiceClient {

    @PostMapping("/email/api/v1/emails/fetch")
    ResponseEntity<String> fetchEmails(@RequestParam("mailboxId") Long mailboxId);
}
