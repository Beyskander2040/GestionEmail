package com.example.mailbox.Service;

import com.example.mailbox.Entity.Mailbox;

import java.util.List;

public interface IMailBox {
    List<Mailbox> getMailboxesByUserId(Long userId);
    void fetchEmailsForMailbox1(String userEmail, String userPassword);
}
