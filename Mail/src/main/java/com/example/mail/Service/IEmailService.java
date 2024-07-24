package com.example.mail.Service;

import com.example.mail.Entity.Mail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IEmailService {
//    void readAndSaveEmails() throws Exception;

    Page<Mail> getEmails(int page, int size);

    //    void readAndSaveEmails1(String userEmail, String userPassword) ;
    List<Mail> getEmailsByUserEmail(String userEmail);

    List<Mail> getEmailsByUserId(Integer userId);

    void readAndSaveEmailsForMailbox(String userEmail, String userPassword, Integer userId);

    boolean readAndSaveEmails1(String userEmail, String userPassword);

    void readAndSaveEmails33(Long mailboxId, String userEmail, String userPassword);
    List<Mail> fetchEmails( String userEmail, String userPassword, int page, int size);
}
