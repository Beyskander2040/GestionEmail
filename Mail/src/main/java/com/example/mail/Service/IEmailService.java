package com.example.mail.Service;

import com.example.mail.Entity.EmailDTO;
import com.example.mail.Entity.Mail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.function.Consumer;

public interface IEmailService {
//    void readAndSaveEmails() throws Exception;

    Page<Mail> getEmails(int page, int size);

    //    void readAndSaveEmails1(String userEmail, String userPassword) ;
    List<Mail> getEmailsByUserEmail(String userEmail);

//    List<Mail> getEmailsByUserId(Integer userId);

//    void readAndSaveEmailsForMailbox(String userEmail, String userPassword, Integer userId);

    boolean readAndSaveEmails1(String userEmail, String userPassword);

    void readAndSaveEmails33(Long mailboxId, String email, String password);
    List<Mail> fetchEmails(String userEmail, String userPassword, int page, int size);
    List<EmailDTO> fetchAndReturnEmails(String userEmail, String userPassword);
    public List<Mail> fetchEmailsabc(String email, String password, int page, int size);

    List<Mail> findByMailboxId(Long mailboxId);
    void checkAndLogNewEmails(String email, String password, Long mailboxId);
}
