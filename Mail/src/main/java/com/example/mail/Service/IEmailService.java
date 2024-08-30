package com.example.mail.Service;

import com.example.mail.Entity.EmailDTO;
import com.example.mail.Entity.Mail;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface IEmailService {
    void readAndSaveEmails33(Long mailboxId, String email, String password);

    List<Mail> findByMailboxId(Long mailboxId);
    void checkAndLogNewEmails(String email, String password, Long mailboxId);
    void archiveEmail(Long mailId, Long mailboxId) ;
    Long getUidFromDatabase(Long mailId);
//   void deleteEmail(String mailUid, Long emailId, Long mailboxId) throws MessagingException ;
List<Map<String, Object>> getTopDomainsWithAttachments();
}
