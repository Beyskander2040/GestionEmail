package com.example.mail.Service;

import com.example.mail.Entity.Mail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IEmailService {
    void readAndSaveEmails() throws Exception;
    Page<Mail> getEmails(int page, int size);
}
