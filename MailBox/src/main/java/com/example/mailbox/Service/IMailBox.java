package com.example.mailbox.Service;

import com.example.mailbox.Entity.Mailbox;

import java.util.List;

public interface IMailBox {
    List<Mailbox> getMailboxesByUserId(Long userId);
    Boolean fetchEmailsForMailbox1(String userEmail, String userPassword);
    void deleteMailbox(Long mailboxId);

}
